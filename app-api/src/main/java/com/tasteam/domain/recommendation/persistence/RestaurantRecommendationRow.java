package com.tasteam.domain.recommendation.persistence;

import java.time.Instant;

public record RestaurantRecommendationRow(
	Long userId,
	Long restaurantId,
	double score,
	int rank,
	Instant generatedAt,
	Instant expiresAt) {
}
