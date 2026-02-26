package com.tasteam.domain.restaurant.dto;

/**
 * 리뷰 감정 분석 결과 (긍정 비율).
 */
public record RestaurantAiSentiment(
	Integer positivePercent) {
}
