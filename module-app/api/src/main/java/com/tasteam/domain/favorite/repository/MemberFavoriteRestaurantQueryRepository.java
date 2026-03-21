package com.tasteam.domain.favorite.repository;

import java.util.List;

import com.tasteam.domain.favorite.dto.FavoriteCursor;
import com.tasteam.domain.favorite.dto.FavoriteRestaurantQueryDto;

public interface MemberFavoriteRestaurantQueryRepository {

	List<FavoriteRestaurantQueryDto> findFavoriteRestaurants(Long memberId, FavoriteCursor cursor, int size);
}
