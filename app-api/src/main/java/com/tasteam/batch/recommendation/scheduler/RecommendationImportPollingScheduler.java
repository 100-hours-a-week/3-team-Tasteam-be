package com.tasteam.batch.recommendation.scheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.tasteam.batch.recommendation.config.RecommendationImportSchedulerProperties;
import com.tasteam.batch.recommendation.runner.RecommendationImportBatchRunner;
import com.tasteam.global.exception.business.BusinessException;
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
		runImport("scheduled");
	}

	@EventListener(ApplicationReadyEvent.class)
	public void pollAndImportOnStartup() {
		runImport("startup");
	}

	private void runImport(String trigger) {
		if (!StringUtils.hasText(schedulerProperties.getS3PrefixOrUri())) {
			log.warn("Recommendation import polling skipped. trigger={}, s3PrefixOrUri is blank.", trigger);
			return;
		}

		Optional<LockHandle> lockHandleOpt = distributedLockManager.tryLock(LOCK_KEY, LOCK_TTL);
		if (lockHandleOpt.isEmpty()) {
			log.info(
				"Recommendation import polling skipped. trigger={}, another instance already owns scheduler lock. key={}",
				trigger,
				LOCK_KEY);
			return;
		}

		String requestId = buildRequestId(trigger);
		try (LockHandle ignored = lockHandleOpt.get()) {
			log.info(
				"Recommendation import polling triggered. trigger={}, modelVersion={}, requestId={}, s3PrefixOrUri={}",
				trigger, schedulerProperties.getModelVersion(), requestId, schedulerProperties.getS3PrefixOrUri());
			try {
				recommendationImportBatchRunner.runOnDemand(
					schedulerProperties.getModelVersion(),
					schedulerProperties.getS3PrefixOrUri(),
					requestId);
			} catch (RuntimeException ex) {
				log.error(
					"Recommendation import polling failed. trigger={}, modelVersion={}, requestId={}, s3PrefixOrUri={}, errorCode={}, message={}",
					trigger,
					schedulerProperties.getModelVersion(),
					requestId,
					schedulerProperties.getS3PrefixOrUri(),
					resolveErrorCode(ex),
					ex.getMessage(),
					ex);
			}
		}
	}

	private String resolveErrorCode(Throwable throwable) {
		if (throwable instanceof BusinessException businessException) {
			return businessException.getErrorCode();
		}
		return throwable.getClass().getSimpleName();
	}

	private String buildRequestId(String trigger) {
		String prefix = StringUtils.hasText(schedulerProperties.getRequestIdPrefix())
			? schedulerProperties.getRequestIdPrefix().trim()
			: "recommendation-import-polling";
		if ("startup".equals(trigger)) {
			return prefix + "-startup-" + UUID.randomUUID();
		}
		return prefix + "-" + LocalDateTime.now().getHour() + "-" + UUID.randomUUID();
	}
}
