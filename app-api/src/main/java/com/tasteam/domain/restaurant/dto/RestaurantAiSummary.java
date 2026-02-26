package com.tasteam.domain.restaurant.dto;

import java.util.Map;

import com.tasteam.domain.restaurant.type.AiReviewCategory;

/**
 * 리뷰 요약 분석 결과 (전체 요약 + 카테고리별 상세).
 */
public record RestaurantAiSummary(
	String overallSummary,
	Map<AiReviewCategory, RestaurantAiCategorySummary> categoryDetails) {
}
