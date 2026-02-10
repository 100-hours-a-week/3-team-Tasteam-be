package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorDeleteRequest(
	@JsonProperty("restaurant_id")
	long restaurantId,
	@JsonProperty("review_id")
	Integer reviewId) {
}
