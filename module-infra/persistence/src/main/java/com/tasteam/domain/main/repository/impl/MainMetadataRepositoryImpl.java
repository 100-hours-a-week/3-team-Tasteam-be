package com.tasteam.domain.main.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.tasteam.domain.main.repository.MainMetadataRepository;
import com.tasteam.domain.restaurant.entity.RestaurantReviewSummary;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSummaryRepository;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MainMetadataRepositoryImpl implements MainMetadataRepository {

	private final RestaurantFoodCategoryRepository categoryRepository;
	private final RestaurantReviewSummaryRepository summaryRepository;

	@Override
	public List<RestaurantCategoryProjection> findCategoriesByRestaurantIds(List<Long> restaurantIds) {
		return categoryRepository.findCategoriesByRestaurantIds(restaurantIds);
	}

	@Override
	public List<RestaurantReviewSummary> findSummariesByRestaurantIdIn(List<Long> restaurantIds) {
		return summaryRepository.findByRestaurantIdIn(restaurantIds);
	}
}
