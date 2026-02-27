package com.tasteam.domain.favorite.dto;

import java.time.Instant;

public record SubgroupFavoriteRestaurantQueryDto(
	Long subgroupFavoriteId,
	Long restaurantId,
	String restaurantName,
	Instant createdAt,
	Long groupFavoriteCount) {
}
