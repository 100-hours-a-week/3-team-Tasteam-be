package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.importer.config.RecommendationImportPollingProperties;
import com.tasteam.infra.storage.StorageClient;

@UnitTest
@DisplayName("[유닛](Recommendation) S3RecommendationResultPollingService 단위 테스트")
class S3RecommendationResultPollingServiceTest {

	@Test
	@DisplayName("pipeline_version 하위에서 _SUCCESS + csv가 있는 최신 dt를 반환한다")
	void awaitImportTarget_returnsLatestCompletedDt() {
		StorageClient storageClient = mock(StorageClient.class);
		RecommendationImportPollingProperties properties = new RecommendationImportPollingProperties();
		properties.setTimeout(Duration.ofSeconds(1));
		properties.setInterval(Duration.ofMillis(10));
		S3RecommendationResultPollingService service = new S3RecommendationResultPollingService(storageClient,
			properties);
		when(storageClient.listObjects("recommendations/pipeline_version=deepfm-1/")).thenReturn(List.of(
			"recommendations/pipeline_version=deepfm-1/dt=2026-03-07/_SUCCESS",
			"recommendations/pipeline_version=deepfm-1/dt=2026-03-07/part-00001.csv",
			"recommendations/pipeline_version=deepfm-1/dt=2026-03-08/_SUCCESS",
			"recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00003.csv",
			"recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00001.csv"));

		RecommendationResultS3Target target = service.awaitImportTarget(
			"s3://tasteam-dev-analytics/recommendations/",
			"deepfm-1",
			"req-1");

		assertThat(target.pipelineVersion()).isEqualTo("deepfm-1");
		assertThat(target.batchDate()).hasToString("2026-03-08");
		assertThat(target.resultFileS3Uri())
			.isEqualTo(
				"s3://tasteam-dev-analytics/recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00001.csv");
	}

	@Test
	@DisplayName("_SUCCESS 없는 dt는 완료 배치로 간주하지 않는다")
	void awaitImportTarget_requiresSuccessMarker() {
		StorageClient storageClient = mock(StorageClient.class);
		RecommendationImportPollingProperties properties = new RecommendationImportPollingProperties();
		properties.setTimeout(Duration.ofMillis(30));
		properties.setInterval(Duration.ofMillis(10));
		S3RecommendationResultPollingService service = new S3RecommendationResultPollingService(storageClient,
			properties);
		when(storageClient.listObjects("recommendations/pipeline_version=deepfm-1/")).thenReturn(List.of(
			"recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00001.csv"));

		assertThatThrownBy(() -> service.awaitImportTarget(
			"s3://tasteam-dev-analytics/recommendations/",
			"deepfm-1",
			"req-2"))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("대기 시간 초과");
	}
}
