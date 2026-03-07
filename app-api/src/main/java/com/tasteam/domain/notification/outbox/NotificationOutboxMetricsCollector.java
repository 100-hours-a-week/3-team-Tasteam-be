package com.tasteam.domain.notification.outbox;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxMetricsCollector {

	private final NotificationOutboxService outboxService;
	@Nullable
	private final MeterRegistry meterRegistry;

	private final AtomicLong pendingCount = new AtomicLong(0);
	private final AtomicLong publishedCount = new AtomicLong(0);
	private final AtomicLong failedCount = new AtomicLong(0);
	private final AtomicLong retryingCount = new AtomicLong(0);

	@PostConstruct
	void init() {
		if (meterRegistry == null) {
			return;
		}
		meterRegistry.gauge("notification.outbox.pending", pendingCount);
		meterRegistry.gauge("notification.outbox.published", publishedCount);
		meterRegistry.gauge("notification.outbox.failed", failedCount);
		meterRegistry.gauge("notification.outbox.retrying", retryingCount);
		refresh();
	}

	@Scheduled(fixedDelayString = "${tasteam.notification.outbox.metrics.refresh-delay:30000}")
	public void refresh() {
		if (meterRegistry == null) {
			return;
		}
		try {
			NotificationOutboxSummary summary = outboxService.summarize();
			pendingCount.set(summary.pendingCount());
			publishedCount.set(summary.publishedCount());
			failedCount.set(summary.failedCount());
			retryingCount.set(summary.retryingCount());
		} catch (Exception ex) {
			log.warn("알림 outbox 메트릭 스냅샷 수집에 실패했습니다.", ex);
			MetricLabelPolicy.validate("notification.outbox.snapshot.error", "reason", "collect_fail");
			meterRegistry.counter("notification.outbox.snapshot.error", "reason", "collect_fail").increment();
		}
	}
}
