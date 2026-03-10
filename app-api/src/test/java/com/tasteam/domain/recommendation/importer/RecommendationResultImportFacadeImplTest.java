package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.importer.config.RecommendationImportPollingProperties;
import com.tasteam.domain.recommendation.repository.RestaurantRecommendationImportCheckpointRepository;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationResultImportFacadeImpl 단위 테스트")
class RecommendationResultImportFacadeImplTest {

	@Test
	@DisplayName("S3 결과 대기 후 import 서비스를 호출한다")
	void importRecommendationResults_callsImportService() {
		S3RecommendationResultPollingService pollingService = mock(S3RecommendationResultPollingService.class);
		RecommendationImportPollingProperties properties = new RecommendationImportPollingProperties();
		properties.setRetryBackoff(Duration.ZERO);
		RestaurantRecommendationImportCheckpointRepository checkpointRepository = mock(
			RestaurantRecommendationImportCheckpointRepository.class);
		RecommendationResultImportService importService = mock(RecommendationResultImportService.class);
		RecommendationResultImportFacadeImpl facade = new RecommendationResultImportFacadeImpl(
			pollingService,
			properties,
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
		RecommendationImportPollingProperties properties = new RecommendationImportPollingProperties();
		properties.setRetryBackoff(Duration.ZERO);
		RestaurantRecommendationImportCheckpointRepository checkpointRepository = mock(
			RestaurantRecommendationImportCheckpointRepository.class);
		RecommendationResultImportService importService = mock(RecommendationResultImportService.class);
		RecommendationResultImportFacadeImpl facade = new RecommendationResultImportFacadeImpl(
			pollingService,
			properties,
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
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("이미 import 완료된 추천 결과");
	}

	@Test
	@DisplayName("폴링 타임아웃은 재시도 후 성공하면 import를 진행한다")
	void importRecommendationResults_retriesWhenPollingTimeout() {
		S3RecommendationResultPollingService pollingService = mock(S3RecommendationResultPollingService.class);
		RecommendationImportPollingProperties properties = new RecommendationImportPollingProperties();
		properties.setPollingMaxAttempts(2);
		properties.setRetryBackoff(Duration.ZERO);
		RestaurantRecommendationImportCheckpointRepository checkpointRepository = mock(
			RestaurantRecommendationImportCheckpointRepository.class);
		RecommendationResultImportService importService = mock(RecommendationResultImportService.class);
		RecommendationResultImportFacadeImpl facade = new RecommendationResultImportFacadeImpl(
			pollingService,
			properties,
			checkpointRepository,
			importService);

		when(pollingService.awaitImportTarget("s3://bucket/recommendations/", "deepfm-1", "req-1"))
			.thenThrow(RecommendationBusinessException.resultPollingTimeout("timeout"))
			.thenReturn(new RecommendationResultS3Target(
				"s3://bucket/recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00001.csv",
				"deepfm-1",
				LocalDate.parse("2026-03-08")));
		when(checkpointRepository.existsByPipelineVersionAndBatchDt("deepfm-1", LocalDate.parse("2026-03-08")))
			.thenReturn(false);
		when(importService.importResults(org.mockito.ArgumentMatchers.any()))
			.thenReturn(new RecommendationResultImportResult("deepfm-1", 2, 2, 0));

		RecommendationResultImportResult result = facade.importResults(
			new RecommendationResultImportFacadeCommand("deepfm-1", "s3://bucket/recommendations/", "req-1"));

		assertThat(result.insertedRows()).isEqualTo(2);
		verify(pollingService, times(2)).awaitImportTarget("s3://bucket/recommendations/", "deepfm-1", "req-1");
	}

	@Test
	@DisplayName("CSV 형식 오류는 재시도하지 않고 즉시 실패한다")
	void importRecommendationResults_notRetryWhenCsvFormatInvalid() {
		S3RecommendationResultPollingService pollingService = mock(S3RecommendationResultPollingService.class);
		RecommendationImportPollingProperties properties = new RecommendationImportPollingProperties();
		properties.setImportMaxAttempts(3);
		properties.setRetryBackoff(Duration.ZERO);
		RestaurantRecommendationImportCheckpointRepository checkpointRepository = mock(
			RestaurantRecommendationImportCheckpointRepository.class);
		RecommendationResultImportService importService = mock(RecommendationResultImportService.class);
		RecommendationResultImportFacadeImpl facade = new RecommendationResultImportFacadeImpl(
			pollingService,
			properties,
			checkpointRepository,
			importService);

		when(pollingService.awaitImportTarget("s3://bucket/recommendations/", "deepfm-1", "req-1"))
			.thenReturn(new RecommendationResultS3Target(
				"s3://bucket/recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00001.csv",
				"deepfm-1",
				LocalDate.parse("2026-03-08")));
		when(checkpointRepository.existsByPipelineVersionAndBatchDt("deepfm-1", LocalDate.parse("2026-03-08")))
			.thenReturn(false);
		when(importService.importResults(org.mockito.ArgumentMatchers.any()))
			.thenThrow(RecommendationBusinessException.csvFormatInvalid("bad-csv"));

		assertThatThrownBy(() -> facade.importResults(
			new RecommendationResultImportFacadeCommand("deepfm-1", "s3://bucket/recommendations/", "req-1")))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("bad-csv");

		verify(importService, times(1)).importResults(org.mockito.ArgumentMatchers.any());
	}
}
