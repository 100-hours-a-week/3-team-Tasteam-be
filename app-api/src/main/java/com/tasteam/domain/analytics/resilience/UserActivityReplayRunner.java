package com.tasteam.domain.analytics.resilience;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.tasteam.global.aop.ObservedAsyncPipeline;
import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class UserActivityReplayRunner {

	private final UserActivityReplayService userActivityReplayService;
	@Nullable
	private final MeterRegistry meterRegistry;

	@ObservedAsyncPipeline(domain = "analytics", stage = "replay_batch")
	public UserActivityReplayResult runPendingReplay(int replayBatchSize) {
		Instant startedAt = Instant.now();
		UserActivityReplayResult result = userActivityReplayService.replayPending(replayBatchSize);
		recordReplayMetrics(result, Duration.between(startedAt, Instant.now()));
		return result;
	}

	private void recordReplayMetrics(UserActivityReplayResult result, Duration batchDuration) {
		if (meterRegistry == null) {
			return;
		}
		if (result.successCount() > 0) {
			MetricLabelPolicy.validate("analytics.user-activity.replay.processed", "result", "success");
			meterRegistry.counter("analytics.user-activity.replay.processed", "result", "success")
				.increment(result.successCount());
		}
		if (result.failedCount() > 0) {
			MetricLabelPolicy.validate("analytics.user-activity.replay.processed", "result", "fail");
			meterRegistry.counter("analytics.user-activity.replay.processed", "result", "fail")
				.increment(result.failedCount());
		}
		Timer.builder("analytics.user-activity.replay.batch.duration")
			.publishPercentileHistogram()
			.register(meterRegistry)
			.record(batchDuration);
	}
}
