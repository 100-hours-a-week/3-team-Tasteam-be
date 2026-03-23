package com.tasteam.batch.recommendation.scheduler;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tasteam.batch.recommendation.config.RecommendationImportSchedulerProperties;
import com.tasteam.batch.recommendation.runner.RecommendationImportBatchRunner;
import com.tasteam.global.lock.RedisDistributedLockManager;
import com.tasteam.global.lock.RedisDistributedLockManager.LockHandle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.batch.recommendation-import", name = "enabled", havingValue = "true")
public class RecommendationImportPollingScheduler {

	private static final String LOCK_KEY = "lock:batch:recommendation-import:polling";
	private static final Duration LOCK_TTL = Duration.ofMinutes(20);

	private final RecommendationImportBatchRunner recommendationImportBatchRunner;
	private final RecommendationImportSchedulerProperties schedulerProperties;
	private final RedisDistributedLockManager distributedLockManager;

	@Scheduled(cron = "${tasteam.batch.recommendation-import.cron:0 */15 * * * ?}", zone = "${tasteam.batch.recommendation-import.zone:Asia/Seoul}")
	public void pollAndImport() {
		if (!StringUtils.hasText(schedulerProperties.getS3PrefixOrUri())) {
			log.warn("Recommendation import polling skipped. s3PrefixOrUri is blank.");
			return;
		}

		Optional<LockHandle> lockHandleOpt = distributedLockManager.tryLock(LOCK_KEY, LOCK_TTL);
		if (lockHandleOpt.isEmpty()) {
			log.info("Recommendation import polling skipped. another instance already owns scheduler lock. key={}",
				LOCK_KEY);
			return;
		}

		String requestId = buildRequestId();
		try (LockHandle ignored = lockHandleOpt.get()) {
			log.info("Recommendation import polling triggered. modelVersion={}, requestId={}, s3PrefixOrUri={}",
				schedulerProperties.getModelVersion(), requestId, schedulerProperties.getS3PrefixOrUri());
			recommendationImportBatchRunner.runOnDemand(
				schedulerProperties.getModelVersion(),
				schedulerProperties.getS3PrefixOrUri(),
				requestId);
		}
	}

	private String buildRequestId() {
		String prefix = StringUtils.hasText(schedulerProperties.getRequestIdPrefix())
			? schedulerProperties.getRequestIdPrefix().trim()
			: "recommendation-import-polling";
		return prefix + "-" + UUID.randomUUID();
	}
}
