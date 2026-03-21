package com.tasteam.domain.analytics.resilience;

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
public class UserActivitySourceOutboxMetricsCollector {

	private final UserActivitySourceOutboxService outboxService;
	@Nullable
	private final MeterRegistry meterRegistry;

	private final AtomicLong pendingCount = new AtomicLong(0);
	private final AtomicLong failedCount = new AtomicLong(0);
	private final AtomicLong publishedCount = new AtomicLong(0);
	private final AtomicLong retryingCount = new AtomicLong(0);

	@PostConstruct
	void init() {
		if (meterRegistry == null) {
			return;
		}
		meterRegistry.gauge("analytics.user-activity.source.outbox.pending", pendingCount);
		meterRegistry.gauge("analytics.user-activity.source.outbox.failed", failedCount);
		meterRegistry.gauge("analytics.user-activity.source.outbox.published", publishedCount);
		meterRegistry.gauge("analytics.user-activity.source.outbox.retrying", retryingCount);
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
			publishedCount.set(summary.publishedCount());
			retryingCount.set(summary.maxRetryCount());
		} catch (Exception ex) {
			log.warn("User Activity source outbox 메트릭 스냅샷 수집에 실패했습니다.", ex);
			MetricLabelPolicy.validate("analytics.user-activity.source.outbox.snapshot.error", "reason",
				"collect_fail");
			meterRegistry.counter("analytics.user-activity.source.outbox.snapshot.error", "reason", "collect_fail")
				.increment();
		}
	}

	public void recordEnqueueResult(String result) {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate("analytics.user-activity.outbox.enqueue", "result", result);
		meterRegistry.counter("analytics.user-activity.outbox.enqueue", "result", result)
			.increment();
	}

	public void recordPublishResult(String result) {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate("analytics.user-activity.outbox.publish", "result", result);
		meterRegistry.counter("analytics.user-activity.outbox.publish", "result", result)
			.increment();
	}

	public void recordRetryScheduled() {
		if (meterRegistry == null) {
			return;
		}
		MetricLabelPolicy.validate("analytics.user-activity.outbox.retry", "result", "scheduled");
		meterRegistry.counter("analytics.user-activity.outbox.retry", "result", "scheduled")
			.increment();
	}
}
