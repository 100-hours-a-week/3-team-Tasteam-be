package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSentimentAnalysisDisplayResponse(
	@JsonProperty("restaurant_id")
	long restaurantId,
	@JsonProperty("positive_ratio")
	int positiveRatio,
	@JsonProperty("negative_ratio")
	int negativeRatio) {
}
