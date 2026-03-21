package com.tasteam.domain.recommendation.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationBusinessException 단위 테스트")
class RecommendationBusinessExceptionTest {

	@Test
	@DisplayName("모델 없음 예외는 NOT_FOUND 상태와 상세 메시지를 가진다")
	void modelNotFound_containsStatusAndMessage() {
		RecommendationBusinessException exception = RecommendationBusinessException.modelNotFound("deepfm-1");

		assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.getErrorCode()).isEqualTo("RECOMMENDATION_MODEL_NOT_FOUND");
		assertThat(exception.getMessage()).contains("version=deepfm-1");
	}

	@Test
	@DisplayName("버전 불일치 예외는 BAD_REQUEST 상태를 가진다")
	void pipelineVersionMismatch_hasBadRequest() {
		RecommendationBusinessException exception = RecommendationBusinessException.pipelineVersionMismatch(
			"deepfm-1",
			"deepfm-2");

		assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(exception.getErrorCode()).isEqualTo("RECOMMENDATION_PIPELINE_VERSION_MISMATCH");
		assertThat(exception.getMessage()).contains("expected=deepfm-1").contains("actual=deepfm-2");
	}
}
