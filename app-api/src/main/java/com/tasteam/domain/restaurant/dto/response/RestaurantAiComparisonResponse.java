package com.tasteam.domain.restaurant.dto.response;

import java.util.Map;

/**
 * 음식점 비교 분석 결과 (API 응답).
 */
public record RestaurantAiComparisonResponse(
	String overallComparison,
	Map<String, AiCategoryComparisonResponse> categoryDetails) {
}
