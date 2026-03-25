package com.tasteam.batch.ai.scheduler;

import java.time.Duration;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.review.runner.ReviewAnalysisBatchRunner;
import com.tasteam.batch.ai.vector.runner.VectorUploadBatchRunner;
import com.tasteam.global.lock.RedisDistributedLockManager;
import com.tasteam.global.lock.RedisDistributedLockManager.LockHandle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매시 30분에 벡터 업로드 + 리뷰 분석 배치를 순서대로 시작.
 * 벡터 실행 생성·Job 디스패치 후, 리뷰 분석 RUNNING 실행을 생성해 둠. 벡터 Job 성공 시 리뷰 Job이 해당 실행에 붙음.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAiBatchScheduler {

	private static final String LOCK_KEY = "lock:batch:ai:daily-scheduler";
	private static final Duration LOCK_TTL = Duration.ofMinutes(10);

	private final VectorUploadBatchRunner vectorUploadBatchRunner;
	private final ReviewAnalysisBatchRunner reviewAnalysisBatchRunner;
	private final RedisDistributedLockManager distributedLockManager;

	@Scheduled(cron = "${tasteam.batch.vector-upload.cron:0 30 * * * ?}", zone = "${tasteam.batch.vector-upload.zone:Asia/Seoul}")
	public void runDailyAiBatch() {
		Optional<LockHandle> lockHandleOpt = distributedLockManager.tryLock(LOCK_KEY, LOCK_TTL);
		if (lockHandleOpt.isEmpty()) {
			log.info("Daily AI batch skipped. another instance already owns scheduler lock. key={}", LOCK_KEY);
			return;
		}

		try (LockHandle ignored = lockHandleOpt.get()) {
			vectorUploadBatchRunner.startRun();
			reviewAnalysisBatchRunner.startRun();
		}
	}
}
