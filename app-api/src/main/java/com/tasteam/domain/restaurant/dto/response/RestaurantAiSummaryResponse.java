package com.tasteam.domain.restaurant.dto.response;

import java.util.Map;

/**
 * 리뷰 요약 분석 결과 (API 응답).
 */
public record RestaurantAiSummaryResponse(
	String overallSummary,
	Map<String, AiCategorySummaryResponse> categoryDetails) {
}
