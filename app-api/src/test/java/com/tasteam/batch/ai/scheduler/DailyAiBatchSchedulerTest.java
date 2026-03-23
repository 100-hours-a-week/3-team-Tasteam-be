package com.tasteam.batch.ai.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.tasteam.batch.ai.review.runner.ReviewAnalysisBatchRunner;
import com.tasteam.batch.ai.vector.runner.VectorUploadBatchRunner;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.global.lock.RedisDistributedLockManager;
import com.tasteam.global.lock.RedisDistributedLockManager.LockHandle;

@UnitTest
@DisplayName("[유닛](Batch) DailyAiBatchScheduler 단위 테스트")
class DailyAiBatchSchedulerTest {

	@Mock
	private VectorUploadBatchRunner vectorUploadBatchRunner;

	@Mock
	private ReviewAnalysisBatchRunner reviewAnalysisBatchRunner;

	@Mock
	private RedisDistributedLockManager distributedLockManager;

	@Mock
	private LockHandle lockHandle;

	@Test
	@DisplayName("락 획득에 성공하면 벡터 업로드와 리뷰 분석 배치를 순서대로 시작한다")
	void runDailyAiBatch_whenLockAcquired_runsBatches() {
		given(distributedLockManager.tryLock(anyString(), any())).willReturn(Optional.of(lockHandle));
		DailyAiBatchScheduler scheduler = new DailyAiBatchScheduler(
			vectorUploadBatchRunner,
			reviewAnalysisBatchRunner,
			distributedLockManager);

		scheduler.runDailyAiBatch();

		then(vectorUploadBatchRunner).should().startRun();
		then(reviewAnalysisBatchRunner).should().startRun();
		then(lockHandle).should().close();
	}

	@Test
	@DisplayName("락 획득에 실패하면 배치를 시작하지 않는다")
	void runDailyAiBatch_whenLockNotAcquired_skipsBatches() {
		given(distributedLockManager.tryLock(anyString(), any())).willReturn(Optional.empty());
		DailyAiBatchScheduler scheduler = new DailyAiBatchScheduler(
			vectorUploadBatchRunner,
			reviewAnalysisBatchRunner,
			distributedLockManager);

		scheduler.runDailyAiBatch();

		then(vectorUploadBatchRunner).should(never()).startRun();
		then(reviewAnalysisBatchRunner).should(never()).startRun();
	}
}
