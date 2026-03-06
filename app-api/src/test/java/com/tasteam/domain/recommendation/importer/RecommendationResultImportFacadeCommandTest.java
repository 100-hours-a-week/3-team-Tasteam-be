package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationResultImportFacadeCommand 단위 테스트")
class RecommendationResultImportFacadeCommandTest {

	@Test
	@DisplayName("모델 버전이 비어있으면 예외를 던진다")
	void throwsWhenRequestedModelVersionBlank() {
		assertThatThrownBy(() -> new RecommendationResultImportFacadeCommand("", "s3://bucket/result.csv", "req-1"))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("요청 모델 버전");
	}

	@Test
	@DisplayName("S3 URI가 s3:// 형식이 아니면 예외를 던진다")
	void throwsWhenResultS3UriInvalid() {
		assertThatThrownBy(
			() -> new RecommendationResultImportFacadeCommand("deepfm-1", "http://bucket/result.csv", "req-1"))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("resultS3Uri");
	}
}
