package com.tasteam.domain.favorite.dto.response;

import java.util.List;

public record RestaurantFavoriteTargetsResponse(
	List<RestaurantFavoriteTargetItem> targets) {
}
