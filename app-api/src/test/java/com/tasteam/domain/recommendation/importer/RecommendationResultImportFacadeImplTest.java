package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationResultImportFacadeImpl 단위 테스트")
class RecommendationResultImportFacadeImplTest {

	@Test
	@DisplayName("S3 결과 대기 후 import 서비스를 호출한다")
	void importRecommendationResults_callsImportService() {
		S3RecommendationResultPollingService pollingService = mock(S3RecommendationResultPollingService.class);
		RecommendationResultImportService importService = mock(RecommendationResultImportService.class);
		RecommendationResultImportFacadeImpl facade = new RecommendationResultImportFacadeImpl(
			pollingService,
			importService);
		RecommendationResultImportResult expected = new RecommendationResultImportResult("deepfm-1", 10, 9, 1);
		when(pollingService.awaitResultS3Uri("s3://bucket/result.csv", "req-1"))
			.thenReturn("s3://bucket/result-0001.csv");
		when(importService.importResults(org.mockito.ArgumentMatchers.any())).thenReturn(expected);

		RecommendationResultImportResult result = facade.importResults(
			new RecommendationResultImportFacadeCommand("deepfm-1", "s3://bucket/result.csv", "req-1"));

		ArgumentCaptor<RecommendationResultImportRequest> captor = ArgumentCaptor.forClass(
			RecommendationResultImportRequest.class);
		verify(importService).importResults(captor.capture());
		RecommendationResultImportRequest request = captor.getValue();
		assertThat(request.requestedModelVersion()).isEqualTo("deepfm-1");
		assertThat(request.s3Uri()).isEqualTo("s3://bucket/result-0001.csv");
		assertThat(request.requestId()).isEqualTo("req-1");
		assertThat(result).isEqualTo(expected);
	}
}
