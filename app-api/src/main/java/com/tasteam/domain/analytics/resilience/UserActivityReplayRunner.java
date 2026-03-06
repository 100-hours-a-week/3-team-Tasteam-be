package com.tasteam.domain.analytics.resilience;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import com.tasteam.global.aop.ObservedAsyncPipeline;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class UserActivityReplayRunner {

	private final UserActivityReplayService userActivityReplayService;

	@ObservedAsyncPipeline(domain = "analytics", stage = "replay_batch")
	public UserActivityReplayResult runPendingReplay(int replayBatchSize) {
		return userActivityReplayService.replayPending(replayBatchSize);
	}
}
