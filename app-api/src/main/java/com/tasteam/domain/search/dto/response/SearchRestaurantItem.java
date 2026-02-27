package com.tasteam.domain.search.dto.response;

import java.util.List;

public record SearchRestaurantItem(
	long restaurantId,
	String name,
	String address,
	String imageUrl,
	List<String> foodCategories) {
}
