package com.tasteam.domain.search.repository;

import java.util.List;

import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;

public interface SearchRestaurantRepository {

	List<RestaurantCategoryProjection> findCategoriesByRestaurantIds(List<Long> ids);
}
