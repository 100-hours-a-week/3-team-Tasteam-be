package com.tasteam.infra.ai.dto;

import java.util.List;

public record AiVectorSearchResponse(
	List<SearchResult> results,
	int total) {
	public record SearchResult(
		AiReviewModel review,
		double score) {
	}
}
