package com.tasteam.domain.restaurant.dto;

/**
 * 음식점 상세용 AI 분석 결과 통합 (감정 + 요약 + 비교).
 */
public record RestaurantAiDetails(
	RestaurantAiSentiment sentiment,
	RestaurantAiSummary summary,
	RestaurantAiComparison comparison) {
}
