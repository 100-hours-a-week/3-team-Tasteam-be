package com.tasteam.domain.analytics.dispatch;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.posthog", name = "enabled", havingValue = "true")
public class UserActivityDispatchOutboxMetricsCollector {

	private final UserActivityDispatchOutboxService outboxService;
	@Nullable
	private final MeterRegistry meterRegistry;

	private final Map<UserActivityDispatchTarget, AtomicLong> pendingByTarget = new EnumMap<>(
		UserActivityDispatchTarget.class);
	private final Map<UserActivityDispatchTarget, AtomicLong> failedByTarget = new EnumMap<>(
		UserActivityDispatchTarget.class);

	@PostConstruct
	void init() {
		if (meterRegistry == null) {
			return;
		}
		for (UserActivityDispatchTarget target : UserActivityDispatchTarget.values()) {
			String targetValue = target.name().toLowerCase();
			MetricLabelPolicy.validate("analytics.user-activity.dispatch.outbox.pending", "target", targetValue);
			MetricLabelPolicy.validate("analytics.user-activity.dispatch.outbox.failed", "target", targetValue);

			AtomicLong pendingGauge = new AtomicLong(0);
			AtomicLong failedGauge = new AtomicLong(0);
			pendingByTarget.put(target, pendingGauge);
			failedByTarget.put(target, failedGauge);

			meterRegistry.gauge("analytics.user-activity.dispatch.outbox.pending",
				Tags.of("target", targetValue),
				pendingGauge);
			meterRegistry.gauge("analytics.user-activity.dispatch.outbox.failed",
				Tags.of("target", targetValue),
				failedGauge);
		}
		refresh();
	}

	@Scheduled(fixedDelayString = "${tasteam.analytics.dispatch-outbox.metrics.refresh-delay:30000}")
	public void refresh() {
		if (meterRegistry == null) {
			return;
		}
		try {
			resetAll();
			List<UserActivityDispatchOutboxSummary> summaries = outboxService.summarizeByTarget();
			for (UserActivityDispatchOutboxSummary summary : summaries) {
				pendingByTarget.get(summary.target()).set(summary.pendingCount());
				failedByTarget.get(summary.target()).set(summary.failedCount());
			}
		} catch (Exception ex) {
			log.warn("User Activity dispatch outbox 메트릭 스냅샷 수집에 실패했습니다.", ex);
			MetricLabelPolicy.validate("analytics.user-activity.dispatch.outbox.snapshot.error", "reason",
				"collect_fail");
			meterRegistry.counter("analytics.user-activity.dispatch.outbox.snapshot.error",
				"reason",
				"collect_fail")
				.increment();
		}
	}

	private void resetAll() {
		pendingByTarget.values().forEach(counter -> counter.set(0));
		failedByTarget.values().forEach(counter -> counter.set(0));
	}

	public void recordEnqueueResult(String result, UserActivityDispatchTarget dispatchTarget) {
		if (meterRegistry == null) {
			return;
		}
		String targetValue = dispatchTarget.name().toLowerCase();
		MetricLabelPolicy.validate("analytics.user-activity.dispatch.enqueue", "result", result);
		MetricLabelPolicy.validate("analytics.user-activity.dispatch.enqueue", "target", targetValue);
		meterRegistry.counter("analytics.user-activity.dispatch.enqueue", "result", result, "target", targetValue)
			.increment();
	}

	public void recordExecuteResult(String result, UserActivityDispatchTarget dispatchTarget) {
		if (meterRegistry == null) {
			return;
		}
		String targetValue = dispatchTarget.name().toLowerCase();
		MetricLabelPolicy.validate("analytics.user-activity.dispatch.execute", "result", result);
		MetricLabelPolicy.validate("analytics.user-activity.dispatch.execute", "target", targetValue);
		meterRegistry.counter("analytics.user-activity.dispatch.execute", "result", result, "target", targetValue)
			.increment();
	}

	public void recordRetryScheduled(UserActivityDispatchTarget dispatchTarget) {
		if (meterRegistry == null) {
			return;
		}
		String targetValue = dispatchTarget.name().toLowerCase();
		MetricLabelPolicy.validate("analytics.user-activity.dispatch.retry", "result", "scheduled");
		MetricLabelPolicy.validate("analytics.user-activity.dispatch.retry", "target", targetValue);
		meterRegistry.counter("analytics.user-activity.dispatch.retry", "result", "scheduled", "target", targetValue)
			.increment();
	}

	public void recordCircuitState(String state, UserActivityDispatchTarget dispatchTarget) {
		if (meterRegistry == null) {
			return;
		}
		String targetValue = dispatchTarget.name().toLowerCase();
		MetricLabelPolicy.validate("analytics.user-activity.dispatch.circuit", "state", state);
		MetricLabelPolicy.validate("analytics.user-activity.dispatch.circuit", "target", targetValue);
		meterRegistry.counter("analytics.user-activity.dispatch.circuit", "state", state, "target", targetValue)
			.increment();
	}
}
