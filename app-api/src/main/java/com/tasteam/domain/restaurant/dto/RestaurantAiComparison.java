package com.tasteam.domain.restaurant.dto;

import java.util.Map;

import com.tasteam.domain.restaurant.type.AiReviewCategory;

/**
 * 음식점 비교 분석 결과 (한 줄 문장 + 카테고리별 lift).
 */
public record RestaurantAiComparison(
	String overallComparison,
	Map<AiReviewCategory, RestaurantAiCategoryComparison> categoryDetails) {
}
