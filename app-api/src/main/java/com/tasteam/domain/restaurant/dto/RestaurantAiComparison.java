package com.tasteam.domain.restaurant.dto;

import java.util.Map;

import com.tasteam.domain.restaurant.type.AiReviewCategory;

/**
 * 음식점 비교 분석 결과 (카테고리별 비교 문장 + lift).
 * comparison_display[i]는 category_lift의 i번째 카테고리와 1:1 매핑.
 */
public record RestaurantAiComparison(
	Map<AiReviewCategory, RestaurantAiCategoryComparison> categoryDetails) {
}
