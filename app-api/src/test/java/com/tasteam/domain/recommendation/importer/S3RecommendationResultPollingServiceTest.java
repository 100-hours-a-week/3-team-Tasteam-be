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
	@DisplayName("prefix에서 csv를 찾으면 해당 s3Uri를 반환한다")
	void awaitResultS3Uri_returnsFirstCsv() {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		RecommendationImportPollingProperties properties = new RecommendationImportPollingProperties();
		properties.setTimeout(Duration.ofSeconds(1));
		properties.setInterval(Duration.ofMillis(10));
		S3RecommendationResultPollingService service = new S3RecommendationResultPollingService(amazonS3, properties);

		ListObjectsV2Result result = new ListObjectsV2Result();
		S3ObjectSummary success = new S3ObjectSummary();
		success.setKey("recommendations/dt=2026-03-06/model=v1/_SUCCESS");
		S3ObjectSummary csv = new S3ObjectSummary();
		csv.setKey("recommendations/dt=2026-03-06/model=v1/part-00001.csv");
		result.getObjectSummaries().add(success);
		result.getObjectSummaries().add(csv);
		when(amazonS3.listObjectsV2(any(com.amazonaws.services.s3.model.ListObjectsV2Request.class)))
			.thenReturn(result);

		String s3Uri = service.awaitResultS3Uri("s3://tasteam-dev-analytics/recommendations/dt=2026-03-06/model=v1/",
			"req-1");

		assertThat(s3Uri).isEqualTo("s3://tasteam-dev-analytics/recommendations/dt=2026-03-06/model=v1/part-00001.csv");
	}

	@Test
	@DisplayName("timeout 내 csv를 찾지 못하면 polling timeout 예외를 던진다")
	void awaitResultS3Uri_throwsWhenTimeout() {
		AmazonS3 amazonS3 = mock(AmazonS3.class);
		RecommendationImportPollingProperties properties = new RecommendationImportPollingProperties();
		properties.setTimeout(Duration.ofMillis(30));
		properties.setInterval(Duration.ofMillis(10));
		S3RecommendationResultPollingService service = new S3RecommendationResultPollingService(amazonS3, properties);

		when(amazonS3.listObjectsV2(any(com.amazonaws.services.s3.model.ListObjectsV2Request.class)))
			.thenReturn(new ListObjectsV2Result());

		assertThatThrownBy(
			() -> service.awaitResultS3Uri("s3://tasteam-dev-analytics/recommendations/dt=2026-03-06/model=v1/",
				"req-2"))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("대기 시간 초과");
	}
}
