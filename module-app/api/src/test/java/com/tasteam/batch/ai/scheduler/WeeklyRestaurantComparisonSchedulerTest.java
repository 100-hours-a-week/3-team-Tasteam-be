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

import com.tasteam.batch.ai.comparison.runner.RestaurantComparisonBatchRunner;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.global.lock.RedisDistributedLockManager;
import com.tasteam.global.lock.RedisDistributedLockManager.LockHandle;

@UnitTest
@DisplayName("[유닛](Batch) WeeklyRestaurantComparisonScheduler 단위 테스트")
class WeeklyRestaurantComparisonSchedulerTest {

	@Mock
	private RestaurantComparisonBatchRunner restaurantComparisonBatchRunner;

	@Mock
	private RedisDistributedLockManager distributedLockManager;

	@Mock
	private LockHandle lockHandle;

	@Test
	@DisplayName("락 획득에 성공하면 주간 레스토랑 비교 배치를 시작한다")
	void runWeeklyRestaurantComparisonBatch_whenLockAcquired_runsBatch() {
		given(distributedLockManager.tryLock(anyString(), any())).willReturn(Optional.of(lockHandle));
		WeeklyRestaurantComparisonScheduler scheduler = new WeeklyRestaurantComparisonScheduler(
			restaurantComparisonBatchRunner,
			distributedLockManager);

		scheduler.runWeeklyRestaurantComparisonBatch();

		then(restaurantComparisonBatchRunner).should().startRun();
		then(lockHandle).should().close();
	}

	@Test
	@DisplayName("락 획득에 실패하면 주간 레스토랑 비교 배치를 시작하지 않는다")
	void runWeeklyRestaurantComparisonBatch_whenLockNotAcquired_skipsBatch() {
		given(distributedLockManager.tryLock(anyString(), any())).willReturn(Optional.empty());
		WeeklyRestaurantComparisonScheduler scheduler = new WeeklyRestaurantComparisonScheduler(
			restaurantComparisonBatchRunner,
			distributedLockManager);

		scheduler.runWeeklyRestaurantComparisonBatch();

		then(restaurantComparisonBatchRunner).should(never()).startRun();
	}
}
