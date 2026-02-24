package com.tasteam.infra.ai.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorUploadRequest(
	List<ReviewPayload> reviews,
	List<RestaurantPayload> restaurants) {
	public record ReviewPayload(
		long id,
		@JsonProperty("restaurant_id")
		long restaurantId,
		String content,
		@JsonProperty("created_at")
		Instant createdAt) {
	}

	public record RestaurantPayload(
		long id,
		String name) {
	}
}
