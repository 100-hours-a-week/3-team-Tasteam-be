package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSummaryRequest(
	@JsonProperty("restaurant_id")
	long restaurantId,
	Integer limit,
	@JsonProperty("min_score")
	Double minScore) {
}
