package com.tasteam.domain.recommendation.importer;

import java.time.Instant;

public record ParsedRecommendationCsvRow(
	long lineNumber,
	String userId,
	String restaurantId,
	String score,
	String rank,
	String pipelineVersion,
	Instant generatedAt,
	Instant expiresAt) {
}
