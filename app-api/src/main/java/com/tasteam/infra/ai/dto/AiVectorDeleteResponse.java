package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorDeleteResponse(
	String action,
	@JsonProperty("review_id")
	Integer reviewId,
	@JsonProperty("point_id")
	String pointId) {
}
