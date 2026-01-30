package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorSearchRequest(
	@JsonProperty("query_text")
	String queryText,
	@JsonProperty("restaurant_id")
	Long restaurantId,
	Integer limit,
	@JsonProperty("min_score")
	Double minScore) {
}
