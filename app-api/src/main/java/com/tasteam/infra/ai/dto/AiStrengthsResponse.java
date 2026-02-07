package com.tasteam.infra.ai.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiStrengthsResponse(
	@JsonProperty("restaurant_id")
	long restaurantId,
	List<StrengthItem> strengths,
	@JsonProperty("total_candidates")
	int totalCandidates,
	@JsonProperty("validated_count")
	int validatedCount,
	@JsonProperty("category_lift")
	Map<String, Double> categoryLift,
	@JsonProperty("strength_display")
	List<String> strengthDisplay) {
	public record StrengthItem(
		String category,
		@JsonProperty("lift_percentage")
		double liftPercentage) {
	}
}
