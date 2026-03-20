package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSentimentBatchRequest(
	List<RestaurantSentimentRequest> restaurants) {

	/**
	 * 레스토랑 1건만 담은 배치 요청. 리뷰는 비움 — 벡터 기반 분석 시 AI 측에서 조회.
	 */
	public static AiSentimentBatchRequest singleRestaurant(long restaurantId) {
		return new AiSentimentBatchRequest(
			List.of(new RestaurantSentimentRequest(restaurantId, List.of())));
	}

	public record RestaurantSentimentRequest(
		@JsonProperty("restaurant_id")
		long restaurantId,
		List<AiSentimentRequest.ReviewContent> reviews) {
	}
}
