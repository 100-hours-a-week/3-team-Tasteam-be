package com.tasteam.domain.recommendation.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;
import com.tasteam.domain.recommendation.persistence.RestaurantRecommendationRow;

@UnitTest
@DisplayName("[유닛](Recommendation) RecommendationImportRowValidator 단위 테스트")
class RecommendationImportRowValidatorTest {

	private final RecommendationImportRowValidator validator = new RecommendationImportRowValidator();

	@Test
	@DisplayName("user_id가 비어 있으면 행을 스킵한다")
	void validateAndConvertOrNull_skipsWhenUserIdBlank() {
		ParsedRecommendationCsvRow row = new ParsedRecommendationCsvRow(
			2L,
			"",
			"101",
			"0.8",
			"1",
			"deepfm-1",
			Instant.parse("2026-02-27T14:00:00Z"),
			Instant.parse("2026-02-28T14:00:00Z"));

		assertThat(validator.validateAndConvertOrNull(row, "deepfm-1")).isNull();
	}

	@Test
	@DisplayName("pipeline_version이 요청 버전과 다르면 예외를 던진다")
	void validateAndConvertOrNull_throwsWhenPipelineMismatch() {
		ParsedRecommendationCsvRow row = new ParsedRecommendationCsvRow(
			2L,
			"1",
			"101",
			"0.8",
			"1",
			"deepfm-2",
			Instant.parse("2026-02-27T14:00:00Z"),
			Instant.parse("2026-02-28T14:00:00Z"));

		assertThatThrownBy(() -> validator.validateAndConvertOrNull(row, "deepfm-1"))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("expected=deepfm-1")
			.hasMessageContaining("actual=deepfm-2");
	}

	@Test
	@DisplayName("expires_at이 generated_at보다 이후가 아니면 예외를 던진다")
	void validateAndConvertOrNull_throwsWhenExpiryInvalid() {
		ParsedRecommendationCsvRow row = new ParsedRecommendationCsvRow(
			2L,
			"1",
			"101",
			"0.8",
			"1",
			"deepfm-1",
			Instant.parse("2026-02-28T14:00:00Z"),
			Instant.parse("2026-02-28T14:00:00Z"));

		assertThatThrownBy(() -> validator.validateAndConvertOrNull(row, "deepfm-1"))
			.isInstanceOf(RecommendationBusinessException.class)
			.hasMessageContaining("expires_at");
	}

	@Test
	@DisplayName("유효한 행이면 RestaurantRecommendationRow로 변환한다")
	void validateAndConvertOrNull_returnsRowWhenValid() {
		ParsedRecommendationCsvRow row = new ParsedRecommendationCsvRow(
			2L,
			"1",
			"101",
			"0.8",
			"1",
			"deepfm-1",
			Instant.parse("2026-02-27T14:00:00Z"),
			Instant.parse("2026-02-28T14:00:00Z"));

		RestaurantRecommendationRow converted = validator.validateAndConvertOrNull(row, "deepfm-1");

		assertThat(converted.userId()).isEqualTo(1L);
		assertThat(converted.restaurantId()).isEqualTo(101L);
		assertThat(converted.rank()).isEqualTo(1);
	}
}
