package com.tasteam.domain.restaurant.repository;

import java.util.List;
import java.util.Set;

import com.tasteam.domain.restaurant.dto.RestaurantCursor;
import com.tasteam.domain.restaurant.dto.RestaurantDistanceQueryDto;

public interface RestaurantQueryRepository {

	List<RestaurantDistanceQueryDto> findRestaurantsWithDistance(
		Long groupId,
		double lat,
		double lng,
		double radiusMeter,
		Set<String> categories,
		RestaurantCursor cursor,
		int pageSize);
}
