package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSummaryBatchRequest(
	List<AiSummaryBatchItem> restaurants) {
	public record AiSummaryBatchItem(
		@JsonProperty("restaurant_id")
		long restaurantId,
		Integer limit) {
	}
}
