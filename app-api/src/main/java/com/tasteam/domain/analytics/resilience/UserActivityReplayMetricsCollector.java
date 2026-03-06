package com.tasteam.domain.analytics.resilience;

import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityReplayMetricsCollector {

	@Nullable
	private final MeterRegistry meterRegistry;

	public void recordReplayResult(UserActivityReplayResult result, long elapsedNanos) {
		if (meterRegistry == null || result == null) {
			return;
		}

		recordCounter("analytics.user-activity.replay.processed", "success", result.successCount());
		recordCounter("analytics.user-activity.replay.processed", "fail", result.failedCount());

		recordDuration("analytics.user-activity.replay.batch.duration", elapsedNanos);
	}

	private void recordCounter(String metricName, String result, long delta) {
		if (delta <= 0) {
			return;
		}
		MetricLabelPolicy.validate(metricName, "result", result);
		meterRegistry.counter(metricName, "result", result).increment(delta);
	}

	private void recordDuration(String metricName, long elapsedNanos) {
		Timer.builder(metricName)
			.publishPercentileHistogram()
			.register(meterRegistry)
			.record(elapsedNanos, TimeUnit.NANOSECONDS);
	}
}
