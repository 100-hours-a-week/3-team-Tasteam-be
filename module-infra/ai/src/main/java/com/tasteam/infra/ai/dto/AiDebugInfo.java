package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiDebugInfo(
	@JsonProperty("request_id")
	String requestId,
	@JsonProperty("processing_time_ms")
	Double processingTimeMs,
	@JsonProperty("tokens_used")
	Integer tokensUsed,
	@JsonProperty("model_version")
	String modelVersion,
	List<String> warnings) {
}
