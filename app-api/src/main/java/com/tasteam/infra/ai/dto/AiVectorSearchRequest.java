package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorSearchRequest(
	@JsonProperty("query_text")
	String queryText,
	@JsonProperty("restaurant_id")
	Long restaurantId,
	Integer limit,
	@JsonProperty("dense_prefetch_limit")
	Integer densePrefetchLimit,
	@JsonProperty("sparse_prefetch_limit")
	Integer sparsePrefetchLimit,
	@JsonProperty("fallback_min_score") @JsonAlias("min_score")
	Double fallbackMinScore) {
}
