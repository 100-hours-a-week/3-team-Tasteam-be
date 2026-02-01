package com.tasteam.domain.restaurant.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tasteam.domain.admin.dto.request.AdminRestaurantSearchCondition;
import com.tasteam.domain.restaurant.dto.RestaurantCursor;
import com.tasteam.domain.restaurant.dto.RestaurantDistanceQueryDto;
import com.tasteam.domain.restaurant.entity.Restaurant;

public interface RestaurantQueryRepository {

	List<RestaurantDistanceQueryDto> findRestaurantsWithDistance(
		double latitude,
		double longitude,
		double radiusMeter,
		Set<String> categories,
		RestaurantCursor cursor,
		int pageSize);

	List<RestaurantDistanceQueryDto> findRestaurantsWithDistance(
		Long groupId,
		double latitude,
		double longitude,
		double radiusMeter,
		Set<String> categories,
		RestaurantCursor cursor,
		int pageSize);

	Page<Restaurant> findAllByAdminCondition(
		AdminRestaurantSearchCondition condition,
		Pageable pageable);
}
