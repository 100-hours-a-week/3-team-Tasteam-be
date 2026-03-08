package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.recommendation.repository.RestaurantRecommendationImportCheckpointRepository;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationResultImportFacadeImpl 단위 테스트")
class RecommendationResultImportFacadeImplTest {

	@Test
	@DisplayName("S3 결과 대기 후 import 서비스를 호출한다")
	void importRecommendationResults_callsImportService() {
		S3RecommendationResultPollingService pollingService = mock(S3RecommendationResultPollingService.class);
		RestaurantRecommendationImportCheckpointRepository checkpointRepository = mock(
			RestaurantRecommendationImportCheckpointRepository.class);
		RecommendationResultImportService importService = mock(RecommendationResultImportService.class);
		RecommendationResultImportFacadeImpl facade = new RecommendationResultImportFacadeImpl(
			pollingService,
			checkpointRepository,
			importService);
		RecommendationResultImportResult expected = new RecommendationResultImportResult("deepfm-1", 10, 9, 1);
		when(pollingService.awaitImportTarget("s3://bucket/recommendations/", "deepfm-1", "req-1"))
			.thenReturn(new RecommendationResultS3Target(
				"s3://bucket/recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00001.csv",
				"deepfm-1",
				LocalDate.parse("2026-03-08")));
		when(checkpointRepository.existsByPipelineVersionAndBatchDt("deepfm-1", LocalDate.parse("2026-03-08")))
			.thenReturn(false);
		when(importService.importResults(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

		RecommendationResultImportResult result = facade.importResults(
			new RecommendationResultImportFacadeCommand("deepfm-1", "s3://bucket/recommendations/", "req-1"));

		ArgumentCaptor<RecommendationResultImportRequest> captor = ArgumentCaptor.forClass(
			RecommendationResultImportRequest.class);
		verify(importService).importResults(captor.capture());
		RecommendationResultImportRequest request = captor.getValue();
		assertThat(request.requestedModelVersion()).isEqualTo("deepfm-1");
		assertThat(request.s3Uri()).isEqualTo(
			"s3://bucket/recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00001.csv");
		assertThat(request.requestId()).isEqualTo("req-1");
		assertThat(result).isEqualTo(expected);
	}

	@Test
	@DisplayName("이미 import된 (pipeline_version, dt) 조합이면 예외를 던진다")
	void importRecommendationResults_throwsWhenAlreadyImported() {
		S3RecommendationResultPollingService pollingService = mock(S3RecommendationResultPollingService.class);
		RestaurantRecommendationImportCheckpointRepository checkpointRepository = mock(
			RestaurantRecommendationImportCheckpointRepository.class);
		RecommendationResultImportService importService = mock(RecommendationResultImportService.class);
		RecommendationResultImportFacadeImpl facade = new RecommendationResultImportFacadeImpl(
			pollingService,
			checkpointRepository,
			importService);

		when(pollingService.awaitImportTarget("s3://bucket/recommendations/", "deepfm-1", "req-1"))
			.thenReturn(new RecommendationResultS3Target(
				"s3://bucket/recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00001.csv",
				"deepfm-1",
				LocalDate.parse("2026-03-08")));
		when(checkpointRepository.existsByPipelineVersionAndBatchDt("deepfm-1", LocalDate.parse("2026-03-08")))
			.thenReturn(true);

		assertThatThrownBy(() -> facade.importResults(
			new RecommendationResultImportFacadeCommand("deepfm-1", "s3://bucket/recommendations/", "req-1")))
			.hasMessageContaining("이미 import 완료된 추천 결과");
	}
}
