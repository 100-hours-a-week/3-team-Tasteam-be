package com.tasteam.domain.restaurant.dto;

import java.util.Set;

public sealed interface RestaurantSearchCondition
	permits GroupRestaurantSearchCondition, NearbyRestaurantSearchCondition {

	Set<String> foodCategories();
}
