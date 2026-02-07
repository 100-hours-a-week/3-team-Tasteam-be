package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSentimentAnalysisResponse(
	@JsonProperty("restaurant_id")
	long restaurantId,
	@JsonProperty("positive_count")
	int positiveCount,
	@JsonProperty("negative_count")
	int negativeCount,
	@JsonProperty("neutral_count")
	int neutralCount,
	@JsonProperty("total_count")
	int totalCount,
	@JsonProperty("positive_ratio")
	int positiveRatio,
	@JsonProperty("negative_ratio")
	int negativeRatio,
	@JsonProperty("neutral_ratio")
	int neutralRatio,
	AiDebugInfo debug) {
}
