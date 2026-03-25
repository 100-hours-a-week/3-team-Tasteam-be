package com.tasteam.infra.ai.dto;

import java.util.List;

public record AiSummaryBatchRequest(
	List<Long> restaurants) {

	/**
	 * 레스토랑 1건만 담은 배치 요청.
	 */
	public static AiSummaryBatchRequest singleRestaurant(long restaurantId) {
		return new AiSummaryBatchRequest(List.of(restaurantId));
	}
}
