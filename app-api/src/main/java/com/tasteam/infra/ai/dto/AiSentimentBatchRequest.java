package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSentimentBatchRequest(
	List<RestaurantSentimentRequest> restaurants) {
	public record RestaurantSentimentRequest(
		@JsonProperty("restaurant_id")
		long restaurantId,
		List<AiSentimentRequest.ReviewContent> reviews) {
	}
}
