package com.tasteam.infra.messagequeue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.resilience.UserActivityReplayResult;
import com.tasteam.domain.analytics.resilience.UserActivityReplayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.analytics.replay", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UserActivityReplayScheduler {

	private final UserActivityReplayService userActivityReplayService;
	@Value("${tasteam.analytics.replay.batch-size:100}")
	private int replayBatchSize;

	@Scheduled(fixedDelayString = "${tasteam.analytics.replay.fixed-delay:PT1M}")
	public void replayPendingEvents() {
		UserActivityReplayResult result = userActivityReplayService.replayPending(replayBatchSize);
		if (result.processedCount() == 0) {
			return;
		}
		log.info("User Activity 재처리 배치 완료. processed={}, success={}, failed={}",
			result.processedCount(), result.successCount(), result.failedCount());
	}
}
