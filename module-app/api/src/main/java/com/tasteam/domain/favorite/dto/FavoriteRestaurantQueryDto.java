package com.tasteam.domain.favorite.dto;

import java.time.Instant;

public record FavoriteRestaurantQueryDto(
	Long favoriteId,
	Long restaurantId,
	String restaurantName,
	Instant createdAt) {
}
