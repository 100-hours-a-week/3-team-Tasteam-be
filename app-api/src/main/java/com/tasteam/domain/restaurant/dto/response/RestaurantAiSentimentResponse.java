package com.tasteam.domain.restaurant.dto.response;

/**
 * 리뷰 감정 분석 결과 (API 응답).
 */
public record RestaurantAiSentimentResponse(
	Integer positivePercent) {
}
