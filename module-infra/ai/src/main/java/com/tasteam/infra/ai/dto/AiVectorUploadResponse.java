package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorUploadResponse(
	String message,
	@JsonProperty("points_count")
	int pointsCount,
	@JsonProperty("collection_name")
	String collectionName) {
}
