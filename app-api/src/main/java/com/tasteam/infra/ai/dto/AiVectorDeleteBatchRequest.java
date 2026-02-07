package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorDeleteBatchRequest(
	@JsonProperty("restaurant_id")
	long restaurantId,
	@JsonProperty("review_ids")
	List<Integer> reviewIds) {
}
