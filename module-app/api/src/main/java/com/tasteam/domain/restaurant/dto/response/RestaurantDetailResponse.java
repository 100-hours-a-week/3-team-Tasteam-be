package com.tasteam.domain.restaurant.dto.response;

import java.time.Instant;
import java.util.List;

public record RestaurantDetailResponse(
	Long id,
	String name,
	String address,
	String phoneNumber,
	List<String> foodCategories,
	List<BusinessHourWeekItem> businessHoursWeek,
	RestaurantImageDto image,
	Boolean isFavorite,
	Long recommendedCount,
	RestaurantAiDetailsResponse aiDetails,
	Instant createdAt,
	Instant updatedAt) {
}
