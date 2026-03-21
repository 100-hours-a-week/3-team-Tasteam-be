package com.tasteam.domain.favorite.dto.response;

import java.time.Instant;
import java.util.List;

public record SubgroupFavoriteRestaurantItem(
	Long restaurantId,
	String name,
	String thumbnailUrl,
	List<String> foodCategories,
	String address,
	Long subgroupId,
	Instant favoritedAt,
	Long groupFavoriteCount) {
}
