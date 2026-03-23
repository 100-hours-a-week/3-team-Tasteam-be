package com.tasteam.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.restaurant.dto.RestaurantAiComparison;
import com.tasteam.domain.restaurant.dto.RestaurantAiSummary;
import com.tasteam.domain.restaurant.type.AiReviewCategory;

@UnitTest
@DisplayName("[유닛](Restaurant) RestaurantAiJsonParser 단위 테스트")
class RestaurantAiJsonParserTest {

	@Test
	@DisplayName("요약 JSON 파싱 시 overall_summary가 없으면 legacy summary 키를 사용한다")
	void parseSummary_usesLegacySummaryKey() {
		// given
		Map<String, Object> summaryJson = Map.of(
			"summary", "레거시 요약",
			"categories", Map.of(
				"food", Map.of(
					"summary", "맛이 진하다",
					"bullets", List.of("불향", "양이 많다"),
					"evidence", List.of(
						Map.of("review_id", "101", "snippet", "불향이 좋아요"),
						Map.of("review_id", 102L, "snippet", "양이 많아요"),
						Map.of("review_id", 103L, "snippet", "세 번째 근거"),
						Map.of("review_id", 104L, "snippet", "잘려야 하는 근거")))));

		// when
		RestaurantAiSummary result = RestaurantAiJsonParser.parseSummary(summaryJson);

		// then
		assertThat(result.overallSummary()).isEqualTo("레거시 요약");
		assertThat(result.categoryDetails()).containsKey(AiReviewCategory.TASTE);
		assertThat(result.categoryDetails().get(AiReviewCategory.TASTE).summary()).isEqualTo("맛이 진하다");
		assertThat(result.categoryDetails().get(AiReviewCategory.TASTE).bullets()).containsExactly("불향", "양이 많다");
		assertThat(result.categoryDetails().get(AiReviewCategory.TASTE).evidences()).hasSize(3);
		assertThat(result.categoryDetails().get(AiReviewCategory.TASTE).evidences().getFirst().reviewId())
			.isEqualTo(101L);
	}

	@Test
	@DisplayName("비교 JSON 파싱 시 strength_display alias도 공통 규칙으로 처리한다")
	void parseComparison_supportsStrengthDisplayAlias() {
		// given
		Map<String, Object> comparisonJson = Map.of(
			"category_lift", Map.of("service", 1.7, "price", "0.8"),
			"strength_display", List.of("서비스 강점", "가격 강점"));

		// when
		RestaurantAiComparison result = RestaurantAiJsonParser.parseComparison(comparisonJson);

		// then
		assertThat(result.categoryDetails()).containsKeys(AiReviewCategory.SERVICE, AiReviewCategory.PRICE);
		assertThat(result.categoryDetails().get(AiReviewCategory.SERVICE).summary()).isEqualTo("서비스 강점");
		assertThat(result.categoryDetails().get(AiReviewCategory.SERVICE).liftScore()).isEqualTo(1.7d);
		assertThat(result.categoryDetails().get(AiReviewCategory.PRICE).summary()).isEqualTo("가격 강점");
		assertThat(result.categoryDetails().get(AiReviewCategory.PRICE).liftScore()).isEqualTo(0.8d);
	}
}
