package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiStrengthsRequest(
	@JsonProperty("restaurant_id")
	long restaurantId,
	@JsonProperty("category_filter")
	String categoryFilter,
	@JsonProperty("region_filter")
	String regionFilter,
	@JsonProperty("price_band_filter")
	String priceBandFilter,
	@JsonProperty("top_k")
	Integer topK,
	@JsonProperty("max_candidates")
	Integer maxCandidates,
	@JsonProperty("months_back")
	Integer monthsBack) {
}
