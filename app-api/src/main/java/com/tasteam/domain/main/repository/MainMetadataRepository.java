package com.tasteam.domain.main.repository;

import java.util.List;

import com.tasteam.domain.restaurant.entity.RestaurantReviewSummary;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;

public interface MainMetadataRepository {

	List<RestaurantCategoryProjection> findCategoriesByRestaurantIds(List<Long> restaurantIds);

	List<RestaurantReviewSummary> findSummariesByRestaurantIdIn(List<Long> restaurantIds);
}
