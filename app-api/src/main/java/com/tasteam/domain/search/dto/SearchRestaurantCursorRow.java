package com.tasteam.domain.search.dto;

import com.tasteam.domain.restaurant.entity.Restaurant;

public record SearchRestaurantCursorRow(
	Restaurant restaurant,
	Integer nameExact,
	Double nameSimilarity,
	Double distanceMeters,
	Integer categoryMatch,
	Integer addressMatch) {
}
