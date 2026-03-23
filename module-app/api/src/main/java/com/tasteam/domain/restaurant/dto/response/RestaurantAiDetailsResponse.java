package com.tasteam.domain.restaurant.dto.response;

/**
 * 음식점 상세용 AI 분석 결과 통합 (API 응답).
 */
public record RestaurantAiDetailsResponse(
	RestaurantAiSentimentResponse sentiment,
	RestaurantAiSummaryResponse summary,
	RestaurantAiComparisonResponse comparison) {
}
