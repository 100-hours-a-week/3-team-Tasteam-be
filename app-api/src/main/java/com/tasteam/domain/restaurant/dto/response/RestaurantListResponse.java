package com.tasteam.domain.restaurant.dto.response;

import java.util.List;

public record RestaurantListResponse(List<RestaurantSummaryResponse> data, CursorPageResponse page) {

	public record RestaurantSummaryResponse(
		long id,
		String name,
		String address,
		double distanceMeter,
		List<String> foodCategories,
		RestaurantImageResponse thumbnailImage) {
	}

	public record RestaurantImageResponse(long id, String url) {
	}
}
