package com.tasteam.domain.restaurant.dto;

/**
 * 리뷰 요약에서 인용하는 근거 리뷰 1건.
 */
public record RestaurantAiEvidence(
	Long reviewId,
	String snippet) {
}
