package com.tasteam.domain.restaurant.dto;

import java.util.Set;

public record GroupRestaurantSearchCondition(
	long groupId,
	double latitude,
	double longitude,
	int radiusMeter,
	Set<String> foodCategories,
	RestaurantCursor cursor,
	int pageSize

) implements RestaurantSearchCondition {
}
