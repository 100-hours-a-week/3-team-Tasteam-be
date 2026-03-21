package com.tasteam.domain.admin.dto.response;

import java.time.Instant;
import java.util.List;

public record AdminRestaurantListItem(
	Long id,
	String name,
	String address,
	List<String> foodCategories,
	Instant createdAt,
	Instant deletedAt) {
}
