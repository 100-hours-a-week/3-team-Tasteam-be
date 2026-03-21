package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationResultImportRequest 단위 테스트")
class RecommendationResultImportRequestTest {

	@Test
	@DisplayName("요청 모델 버전이 비어 있으면 예외를 던진다")
	void constructor_throwsWhenModelVersionBlank() {
		assertThatThrownBy(() -> new RecommendationResultImportRequest("  ", "s3://bucket/path.csv", null))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("요청 모델 버전");
	}

	@Test
	@DisplayName("S3 URI 형식이 아니면 예외를 던진다")
	void constructor_throwsWhenS3UriInvalid() {
		assertThatThrownBy(
			() -> new RecommendationResultImportRequest("deepfm-1", "https://example.com/file.csv", null))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("s3Uri");
	}
}
