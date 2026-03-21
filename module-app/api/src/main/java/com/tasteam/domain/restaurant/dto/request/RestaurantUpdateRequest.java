package com.tasteam.domain.restaurant.dto.request;

import java.util.List;
import java.util.UUID;

public record RestaurantUpdateRequest(
	String name,
	List<Long> foodCategoryIds,
	List<UUID> imageIds) {
}
