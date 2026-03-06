package com.tasteam.domain.recommendation.persistence;

import java.time.Instant;

public record RestaurantRecommendationRow(
	Long memberId,
	String anonymousId,
	Long restaurantId,
	double score,
	int rank,
	String contextSnapshot,
	String pipelineVersion,
	Instant generatedAt,
	Instant expiresAt) {
}
