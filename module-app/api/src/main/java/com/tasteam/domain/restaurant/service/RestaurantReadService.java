package com.tasteam.domain.restaurant.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.entity.FoodCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.entity.RestaurantFoodCategory;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantReadService {

	private final RestaurantRepository restaurantRepository;
	private final RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;
	private final ReviewRepository reviewRepository;

	@Transactional(readOnly = true)
	public RestaurantReadResult readRestaurantDetail(long restaurantId) {
		Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

		List<String> foodCategories = restaurantFoodCategoryRepository.findByRestaurantId(restaurantId)
			.stream()
			.map(RestaurantFoodCategory::getFoodCategory)
			.map(FoodCategory::getName)
			.toList();

		long recommendedCount = reviewRepository
			.countByRestaurantIdAndIsRecommendedTrueAndDeletedAtIsNull(restaurantId);
		long notRecommendedCount = reviewRepository
			.countByRestaurantIdAndIsRecommendedFalseAndDeletedAtIsNull(restaurantId);

		return new RestaurantReadResult(restaurant, foodCategories, recommendedCount, notRecommendedCount);
	}

	public record RestaurantReadResult(
		Restaurant restaurant,
		List<String> foodCategories,
		long recommendedCount,
		long notRecommendedCount) {
	}
}
