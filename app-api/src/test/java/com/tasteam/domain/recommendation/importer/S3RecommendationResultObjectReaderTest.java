package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.config.AnalyticsProperties;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.infra.storage.StorageClient;

@UnitTest
@DisplayName("[유닛](Recommendation) S3RecommendationResultObjectReader 단위 테스트")
class S3RecommendationResultObjectReaderTest {

	@Test
	@DisplayName("analytics bucket 기준으로 추천 결과 파일을 읽는다")
	void openStream_readsFromAnalyticsBucket() throws IOException {
		StorageClient storageClient = mock(StorageClient.class);
		S3RecommendationResultObjectReader reader = new S3RecommendationResultObjectReader(storageClient);
		AnalyticsProperties analyticsProperties = new AnalyticsProperties();
		analyticsProperties.setBucket("tasteam-dev-analytics");
		reader.setAnalyticsProperties(analyticsProperties);
		when(storageClient.downloadObject("tasteam-dev-analytics", "recommendations/dt=2026-03-10/part-00001.csv"))
			.thenReturn("hello".getBytes(StandardCharsets.UTF_8));

		try (var inputStream = reader.openStream(
			"s3://any-bucket/recommendations/dt=2026-03-10/part-00001.csv")) {
			assertThat(inputStream.readAllBytes()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
		}
		verify(storageClient).downloadObject(
			eq("tasteam-dev-analytics"),
			eq("recommendations/dt=2026-03-10/part-00001.csv"));
	}

	@Test
	@DisplayName("analytics bucket 설정이 없으면 실패한다")
	void openStream_failsWhenAnalyticsBucketMissing() {
		StorageClient storageClient = mock(StorageClient.class);
		S3RecommendationResultObjectReader reader = new S3RecommendationResultObjectReader(storageClient);

		assertThatThrownBy(() -> reader.openStream("s3://any-bucket/recommendations/dt=2026-03-10/part-00001.csv"))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("analytics bucket 설정이 비어 있습니다");
	}
}
