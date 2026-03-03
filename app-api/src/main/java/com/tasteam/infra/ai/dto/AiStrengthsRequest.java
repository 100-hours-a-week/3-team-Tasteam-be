package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiStrengthsRequest(
	@JsonProperty("restaurant_id")
	long restaurantId,
	@JsonProperty("top_k")
	Integer topK) {
}
