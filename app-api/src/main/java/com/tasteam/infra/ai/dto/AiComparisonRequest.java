package com.tasteam.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 /api/v1/llm/comparison 요청 스키마 (ComparisonRequest).
 * restaurant_id 만 필수.
 */
public record AiComparisonRequest(
	@JsonProperty("restaurant_id")
	long restaurantId) {
}
