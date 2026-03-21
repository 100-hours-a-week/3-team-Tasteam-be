package com.tasteam.domain.recommendation.importer;

import java.time.Instant;

public record ParsedRecommendationCsvRow(
	long lineNumber,
	String userId,
	String anonymousId,
	String restaurantId,
	String score,
	String rank,
	String contextSnapshot,
	String pipelineVersion,
	Instant generatedAt,
	Instant expiresAt) {
}
