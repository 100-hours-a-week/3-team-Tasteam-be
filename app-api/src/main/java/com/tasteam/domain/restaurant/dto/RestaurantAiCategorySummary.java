package com.tasteam.domain.restaurant.dto;

import java.util.List;

/**
 * 카테고리(맛/가격/서비스)별 리뷰 요약 상세.
 */
public record RestaurantAiCategorySummary(
	String summary,
	List<String> bullets,
	List<RestaurantAiEvidence> evidences) {
}
