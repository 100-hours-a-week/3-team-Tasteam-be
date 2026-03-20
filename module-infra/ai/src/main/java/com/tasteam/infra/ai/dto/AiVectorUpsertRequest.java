package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiVectorUpsertRequest(
	List<AiVectorUploadRequest.ReviewPayload> reviews,
	List<AiVectorUploadRequest.RestaurantPayload> restaurants,
	@JsonProperty("batch_size")
	Integer batchSize) {
}
