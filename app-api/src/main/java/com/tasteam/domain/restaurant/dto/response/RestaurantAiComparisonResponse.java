package com.tasteam.domain.restaurant.dto.response;

import java.util.Map;

/**
 * 음식점 비교 분석 결과 (API 응답). 카테고리별 비교 문장 + lift.
 */
public record RestaurantAiComparisonResponse(
	Map<String, AiCategoryComparisonResponse> categoryDetails) {
}
