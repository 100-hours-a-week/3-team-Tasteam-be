package com.tasteam.domain.favorite.dto.response;

import java.time.Instant;

public record SubgroupFavoriteRestaurantItem(
	Long restaurantId,
	String name,
	String thumbnailUrl,
	Long subgroupId,
	Instant favoritedAt) {
}
