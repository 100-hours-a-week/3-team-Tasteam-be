package com.tasteam.domain.admin.dto.request;

import java.util.List;
import java.util.UUID;

public record AdminRestaurantUpdateRequest(
	String name,
	String address,

	List<Long> foodCategoryIds,

	List<UUID> imageIds) {
}
