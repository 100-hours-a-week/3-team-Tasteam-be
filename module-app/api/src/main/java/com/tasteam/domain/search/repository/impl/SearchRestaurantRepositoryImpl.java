package com.tasteam.domain.search.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;
import com.tasteam.domain.search.repository.SearchRestaurantRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SearchRestaurantRepositoryImpl implements SearchRestaurantRepository {

	private final RestaurantFoodCategoryRepository categoryRepository;

	@Override
	public List<RestaurantCategoryProjection> findCategoriesByRestaurantIds(List<Long> ids) {
		return categoryRepository.findCategoriesByRestaurantIds(ids);
	}
}
