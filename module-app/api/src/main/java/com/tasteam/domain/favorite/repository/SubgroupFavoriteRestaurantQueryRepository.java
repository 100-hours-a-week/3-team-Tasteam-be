package com.tasteam.domain.favorite.repository;

import java.util.List;

import com.tasteam.domain.favorite.dto.SubgroupFavoriteCursor;
import com.tasteam.domain.favorite.dto.SubgroupFavoriteRestaurantQueryDto;

public interface SubgroupFavoriteRestaurantQueryRepository {

	List<SubgroupFavoriteRestaurantQueryDto> findFavoriteRestaurants(Long subgroupId, SubgroupFavoriteCursor cursor,
		int size);
}
