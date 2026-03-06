package com.tasteam.domain.analytics.resilience;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class UserActivitySourceOutboxMetricsCollector {

	private final UserActivitySourceOutboxService outboxService;
	@Nullable
	private final MeterRegistry meterRegistry;

	private final AtomicLong pendingCount = new AtomicLong(0);
	private final AtomicLong failedCount = new AtomicLong(0);

	@PostConstruct
	void init() {
		if (meterRegistry == null) {
			return;
		}
		meterRegistry.gauge("analytics.user-activity.source.outbox.pending", pendingCount);
		meterRegistry.gauge("analytics.user-activity.source.outbox.failed", failedCount);
		refresh();
	}

	@Scheduled(fixedDelayString = "${tasteam.analytics.source-outbox.metrics.refresh-delay:30000}")
	public void refresh() {
		if (meterRegistry == null) {
			return;
		}
		try {
			UserActivitySourceOutboxSummary summary = outboxService.summarize();
			pendingCount.set(summary.pendingCount());
			failedCount.set(summary.failedCount());
		} catch (Exception ex) {
			log.warn("User Activity source outbox 메트릭 스냅샷 수집에 실패했습니다.", ex);
			MetricLabelPolicy.validate("analytics.user-activity.source.outbox.snapshot.error", "reason",
				"collect_fail");
			meterRegistry.counter("analytics.user-activity.source.outbox.snapshot.error", "reason", "collect_fail")
				.increment();
		}
	}
}
