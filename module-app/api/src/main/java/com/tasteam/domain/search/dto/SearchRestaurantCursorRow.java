package com.tasteam.domain.search.dto;

public record SearchRestaurantCursorRow(
	RestaurantSearchRow restaurant,
	Integer nameExact,
	Double nameSimilarity,
	Double ftsRank,
	Double distanceMeters,
	Integer categoryMatch,
	Integer addressMatch) {
}
