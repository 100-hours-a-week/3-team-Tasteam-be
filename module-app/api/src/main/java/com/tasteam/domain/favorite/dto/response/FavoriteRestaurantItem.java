package com.tasteam.domain.favorite.dto.response;

import java.time.Instant;
import java.util.List;

public record FavoriteRestaurantItem(
	long restaurantId,
	String name,
	String thumbnailUrl,
	List<String> foodCategories,
	String address,
	Instant createdAt) {
}
