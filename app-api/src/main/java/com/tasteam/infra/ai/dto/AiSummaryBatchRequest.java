package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSummaryBatchRequest(
	List<AiSummaryBatchItem> restaurants,
	Integer limit,
	@JsonProperty("min_score")
	Double minScore) {
	public record AiSummaryBatchItem(
		@JsonProperty("restaurant_id")
		long restaurantId) {
	}
}
