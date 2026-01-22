package com.tasteam.domain.search.dto.response;

import java.util.List;

import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;

public record SearchRestaurantItem(
	long id,
	String name,
	String address,
	List<String> foodCategories,
	List<RestaurantImageDto> thumbnailImages) {
}
