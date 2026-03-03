package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSummaryBatchRequest(
	List<AiSummaryBatchItem> restaurants,
	Integer limit,
	@JsonProperty("min_score")
	Double minScore) {

	private static final int DEFAULT_LIMIT = 10;

	/**
	 * 레스토랑 1건만 담은 배치 요청. limit은 API 필수, summary-batch-size(기본 10)와 동일.
	 */
	public static AiSummaryBatchRequest singleRestaurant(long restaurantId) {
		return new AiSummaryBatchRequest(
			List.of(new AiSummaryBatchItem(restaurantId)), DEFAULT_LIMIT, null);
	}

	public record AiSummaryBatchItem(
		@JsonProperty("restaurant_id")
		long restaurantId) {
	}
}
