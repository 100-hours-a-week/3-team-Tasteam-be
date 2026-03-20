package com.tasteam.infra.ai.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSummaryResponse(
	@JsonProperty("restaurant_id")
	long restaurantId,
	@JsonProperty("overall_summary")
	String overallSummary,
	Map<String, AiSummaryDisplayResponse.CategorySummary> categories,
	@JsonProperty("positive_reviews")
	List<AiReviewModel> positiveReviews,
	@JsonProperty("negative_reviews")
	List<AiReviewModel> negativeReviews,
	@JsonProperty("positive_count")
	Integer positiveCount,
	@JsonProperty("negative_count")
	Integer negativeCount,
	AiDebugInfo debug) {
}
