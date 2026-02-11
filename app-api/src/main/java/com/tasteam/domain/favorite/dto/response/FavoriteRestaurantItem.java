package com.tasteam.domain.favorite.dto.response;

import java.time.Instant;

public record FavoriteRestaurantItem(
	long restaurantId,
	String name,
	String thumbnailUrl,
	String category,
	String address,
	Instant createdAt) {
}
