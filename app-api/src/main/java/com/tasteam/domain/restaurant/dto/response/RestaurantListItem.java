package com.tasteam.domain.restaurant.dto.response;

import java.util.List;

public record RestaurantListItem(
	long id,
	String name,
	String address,
	double distanceMeter,
	List<String> foodCategories,
	List<RestaurantImageDto> thumbnailImages) {
}
