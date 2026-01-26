package com.tasteam.domain.restaurant.dto.response;

import java.time.Instant;
import java.util.List;

public record RestaurantDetailResponse(
	Long id,
	String name,
	String address,
	List<String> foodCategories,
	List<BusinessHourResponse> businessHours,
	RestaurantImageDto image,
	Boolean isFavorite,
	RecommendStatResponse recommendStat,
	String aiSummary,
	String aiFeatures,
	Instant createdAt,
	Instant updatedAt) {

	public record BusinessHourResponse(String day, String open, String close) {
	}

	public record RecommendStatResponse(Long recommendedCount, Long notRecommendedCount, Long positiveRatio) {
	}
}
