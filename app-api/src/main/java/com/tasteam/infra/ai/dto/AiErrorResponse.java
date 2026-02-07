package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiErrorResponse(
	Integer code,
	String message,
	Object details,
	@JsonProperty("request_id")
	String requestId) {
}
