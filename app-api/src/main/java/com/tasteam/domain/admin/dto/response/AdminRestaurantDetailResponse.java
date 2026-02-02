package com.tasteam.domain.admin.dto.response;

import java.time.Instant;
import java.util.List;

import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;

public record AdminRestaurantDetailResponse(
	Long id,
	String name,
	String address,
	Double latitude,
	Double longitude,
	List<FoodCategoryInfo> foodCategories,
	List<RestaurantImageDto> images,
	Instant createdAt,
	Instant updatedAt,
	Instant deletedAt) {
	public record FoodCategoryInfo(
		Long id,
		String name,
		String code) {
	}
}
