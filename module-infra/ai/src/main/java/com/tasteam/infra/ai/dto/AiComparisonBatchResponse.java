package com.tasteam.infra.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버 /api/v1/llm/comparison/batch 응답 스키마 (ComparisonBatchResponse).
 * results: 각 레스토랑별 ComparisonResponse와 동일 구조.
 */
public record AiComparisonBatchResponse(
	@JsonProperty("results")
	List<AiStrengthsResponse> results) {
}
