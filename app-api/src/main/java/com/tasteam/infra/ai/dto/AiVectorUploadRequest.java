package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorUploadRequest(
	List<ReviewPayload> reviews,
	List<RestaurantPayload> restaurants) {
	public record ReviewPayload(
		Integer id,
		@JsonProperty("restaurant_id")
		long restaurantId,
		String content) {
	}

	public record RestaurantPayload(
		Integer id,
		String name) {
	}
}
