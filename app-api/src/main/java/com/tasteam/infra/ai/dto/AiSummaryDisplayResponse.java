package com.tasteam.infra.ai.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSummaryDisplayResponse(
	@JsonProperty("restaurant_id")
	long restaurantId,
	@JsonProperty("overall_summary")
	String overallSummary,
	Map<String, CategorySummary> categories) {
	public record CategorySummary(
		String summary,
		List<String> bullets,
		List<Evidence> evidence) {
	}

	public record Evidence(
		@JsonProperty("review_id")
		String reviewId,
		String snippet,
		int rank) {
	}
}
