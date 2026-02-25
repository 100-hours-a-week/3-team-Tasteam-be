package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 /api/v1/llm/comparison/batch 요청 스키마 (ComparisonBatchRequest).
 */
public record AiComparisonBatchRequest(
	@JsonProperty("restaurants")
	List<RestaurantIdItem> restaurants,
	@JsonProperty("all_average_data_path")
	String allAverageDataPath) {

	public record RestaurantIdItem(
		@JsonProperty("restaurant_id")
		long restaurantId) {
	}

	public static AiComparisonBatchRequest of(List<Long> restaurantIds) {
		List<RestaurantIdItem> items = restaurantIds.stream()
			.map(RestaurantIdItem::new)
			.toList();
		return new AiComparisonBatchRequest(items, null);
	}
}
