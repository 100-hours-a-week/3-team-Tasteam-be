package com.tasteam.domain.restaurant.dto;

import java.util.List;

/**
 * 카테고리(맛/가격/서비스)별 비교 분석 상세.
 * 요약 상세와 동일한 구조(summary, bullets, evidences) + liftScore.
 */
public record RestaurantAiCategoryComparison(
	String summary,
	List<String> bullets,
	List<RestaurantAiEvidence> evidences,
	Double liftScore) {
}
