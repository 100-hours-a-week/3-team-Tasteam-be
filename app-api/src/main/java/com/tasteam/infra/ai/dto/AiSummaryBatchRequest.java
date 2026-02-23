package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSummaryBatchRequest(
	List<AiSummaryBatchItem> restaurants,
	Integer limit,
	@JsonProperty("min_score")
	Double minScore) {

	/**
	 * 레스토랑 1건만 담은 배치 요청. limit, min_score는 null(API 기본값 사용).
	 */
	public static AiSummaryBatchRequest singleRestaurant(long restaurantId) {
		return new AiSummaryBatchRequest(
			List.of(new AiSummaryBatchItem(restaurantId)), null, null);
	}

	public record AiSummaryBatchItem(
		@JsonProperty("restaurant_id")
		long restaurantId) {
	}
}
