package com.tasteam.domain.restaurant.validator;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.domain.restaurant.repository.FoodCategoryRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestaurantFoodCategoryValidator {

	private final FoodCategoryRepository foodCategoryRepository;

	public void validate(List<Long> categories) {
		if (categories == null || categories.isEmpty()) {
			return;
		}

		// DB에 존재하는 카테고리 수
		long validCount = foodCategoryRepository.countByIdIn(categories);

		if (validCount != categories.size()) {
			throw new BusinessException(RestaurantErrorCode.FOOD_CATEGORY_NOT_FOUND);
		}
	}
}
