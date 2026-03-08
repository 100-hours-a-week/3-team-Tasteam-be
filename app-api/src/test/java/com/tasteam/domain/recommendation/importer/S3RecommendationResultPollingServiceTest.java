package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.importer.config.RecommendationImportPollingProperties;

@UnitTest
@DisplayName("[유닛](Recommendation) S3RecommendationResultPollingService 단위 테스트")
class S3RecommendationResultPollingServiceTest {

	@Test
	@DisplayName("pipeline_version 하위에서 _SUCCESS + csv가 있는 최신 dt를 반환한다")
	void awaitImportTarget_returnsLatestCompletedDt() {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		RecommendationImportPollingProperties properties = new RecommendationImportPollingProperties();
		properties.setTimeout(Duration.ofSeconds(1));
		properties.setInterval(Duration.ofMillis(10));
		S3RecommendationResultPollingService service = new S3RecommendationResultPollingService(amazonS3, properties);

		ListObjectsV2Result result = new ListObjectsV2Result();
		result.getObjectSummaries().add(object("recommendations/pipeline_version=deepfm-1/dt=2026-03-07/_SUCCESS"));
		result.getObjectSummaries()
			.add(object("recommendations/pipeline_version=deepfm-1/dt=2026-03-07/part-00001.csv"));
		result.getObjectSummaries().add(object("recommendations/pipeline_version=deepfm-1/dt=2026-03-08/_SUCCESS"));
		result.getObjectSummaries()
			.add(object("recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00003.csv"));
		result.getObjectSummaries()
			.add(object("recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00001.csv"));
		when(amazonS3.listObjectsV2(any(com.amazonaws.services.s3.model.ListObjectsV2Request.class)))
			.thenReturn(result);

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
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		RecommendationImportPollingProperties properties = new RecommendationImportPollingProperties();
		properties.setTimeout(Duration.ofMillis(30));
		properties.setInterval(Duration.ofMillis(10));
		S3RecommendationResultPollingService service = new S3RecommendationResultPollingService(amazonS3, properties);

		ListObjectsV2Result result = new ListObjectsV2Result();
		result.getObjectSummaries()
			.add(object("recommendations/pipeline_version=deepfm-1/dt=2026-03-08/part-00001.csv"));
		when(amazonS3.listObjectsV2(any(com.amazonaws.services.s3.model.ListObjectsV2Request.class)))
			.thenReturn(result);

		assertThatThrownBy(() -> service.awaitImportTarget(
			"s3://tasteam-dev-analytics/recommendations/",
			"deepfm-1",
			"req-2"))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("대기 시간 초과");
	}

	private S3ObjectSummary object(String key) {
		S3ObjectSummary summary = new S3ObjectSummary();
		summary.setKey(key);
		return summary;
	}
}
