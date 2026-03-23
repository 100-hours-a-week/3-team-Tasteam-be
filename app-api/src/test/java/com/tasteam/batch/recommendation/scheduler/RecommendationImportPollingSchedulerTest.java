package com.tasteam.batch.recommendation.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.tasteam.batch.recommendation.config.RecommendationImportSchedulerProperties;
import com.tasteam.batch.recommendation.runner.RecommendationImportBatchRunner;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.global.lock.RedisDistributedLockManager;
import com.tasteam.global.lock.RedisDistributedLockManager.LockHandle;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationImportPollingScheduler 단위 테스트")
class RecommendationImportPollingSchedulerTest {

	@Mock
	private RecommendationImportBatchRunner recommendationImportBatchRunner;

	@Mock
	private RedisDistributedLockManager distributedLockManager;

	@Mock
	private LockHandle lockHandle;

	@Test
	@DisplayName("락 획득에 성공하면 설정된 S3 prefix와 모델 버전으로 추천 import를 실행한다")
	void pollAndImport_whenLockAcquired_runsImport() {
		RecommendationImportSchedulerProperties properties = new RecommendationImportSchedulerProperties();
		properties.setS3PrefixOrUri("s3://bucket/recommendations/");
		properties.setModelVersion("deepfm-v2");
		properties.setRequestIdPrefix("recommendation-import-polling");
		given(distributedLockManager.tryLock(anyString(), any())).willReturn(Optional.of(lockHandle));
		RecommendationImportPollingScheduler scheduler = new RecommendationImportPollingScheduler(
			recommendationImportBatchRunner,
			properties,
			distributedLockManager);

		scheduler.pollAndImport();

		then(recommendationImportBatchRunner).should().runOnDemand(
			anyString(),
			anyString(),
			anyString());
		then(lockHandle).should().close();
	}

	@Test
	@DisplayName("S3 prefix가 비어 있으면 락을 시도하지 않고 건너뛴다")
	void pollAndImport_whenPrefixBlank_skips() {
		RecommendationImportSchedulerProperties properties = new RecommendationImportSchedulerProperties();
		RecommendationImportPollingScheduler scheduler = new RecommendationImportPollingScheduler(
			recommendationImportBatchRunner,
			properties,
			distributedLockManager);

		scheduler.pollAndImport();

		then(distributedLockManager).shouldHaveNoInteractions();
		then(recommendationImportBatchRunner).should(never()).runOnDemand(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("락 획득에 실패하면 추천 import를 실행하지 않는다")
	void pollAndImport_whenLockNotAcquired_skips() {
		RecommendationImportSchedulerProperties properties = new RecommendationImportSchedulerProperties();
		properties.setS3PrefixOrUri("s3://bucket/recommendations/");
		given(distributedLockManager.tryLock(anyString(), any())).willReturn(Optional.empty());
		RecommendationImportPollingScheduler scheduler = new RecommendationImportPollingScheduler(
			recommendationImportBatchRunner,
			properties,
			distributedLockManager);

		scheduler.pollAndImport();

		then(recommendationImportBatchRunner).should(never()).runOnDemand(anyString(), anyString(), anyString());
	}
}
