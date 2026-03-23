package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorUpsertResponse(
	List<UpsertResult> results,
	int total,
	@JsonProperty("success_count")
	int successCount,
	@JsonProperty("error_count")
	int errorCount) {
	public record UpsertResult(
		String action,
		@JsonProperty("review_id")
		Integer reviewId,
		Integer version,
		@JsonProperty("point_id")
		String pointId) {
	}
}
