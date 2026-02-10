package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorDeleteBatchResponse(
	List<DeleteResult> results,
	int total,
	@JsonProperty("deleted_count")
	int deletedCount,
	@JsonProperty("not_found_count")
	int notFoundCount) {
	public record DeleteResult(
		@JsonProperty("review_id")
		Integer reviewId,
		String action,
		@JsonProperty("point_id")
		String pointId) {
	}
}
