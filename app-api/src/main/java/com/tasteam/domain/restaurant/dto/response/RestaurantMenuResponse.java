package com.tasteam.domain.restaurant.dto.response;

import java.util.List;

public record RestaurantMenuResponse(
	Long restaurantId,
	List<MenuCategoryResponse> categories) {
}
