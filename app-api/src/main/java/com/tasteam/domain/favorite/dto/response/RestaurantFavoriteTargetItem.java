package com.tasteam.domain.favorite.dto.response;

import com.tasteam.domain.favorite.type.FavoriteState;
import com.tasteam.domain.favorite.type.FavoriteTargetType;

public record RestaurantFavoriteTargetItem(
	FavoriteTargetType targetType,
	Long targetId,
	String name,
	FavoriteState favoriteState) {
}
