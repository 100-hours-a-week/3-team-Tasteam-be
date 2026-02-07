package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSentimentRequest(
	@JsonProperty("restaurant_id")
	long restaurantId,
	List<ReviewContent> reviews) {
	public record ReviewContent(
		Integer id,
		@JsonProperty("restaurant_id")
		long restaurantId,
		String content) {
	}
}
