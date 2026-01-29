package com.tasteam.domain.restaurant.validator;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.tasteam.domain.restaurant.dto.RestaurantSearchCondition;
import com.tasteam.domain.restaurant.repository.FoodCategoryRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GroupRestaurantSearchConditionValidator {

	// private final GroupRepository groupRepository;
	private final FoodCategoryRepository foodCategoryRepository;

	public void validate(RestaurantSearchCondition condition) {
		// validateGroupExists(condition.groupId());
		validateFoodCategories(condition.foodCategories());
	}

	/*
	private void validateGroupExists(long groupId) {
	    boolean exists = groupRepository.existsByIdAndDeletedAtIsNull(groupId);
	    if (!exists) {
	        throw new BusinessException(GroupErrorCode.GROUP_NOT_FOUND);
	    }
	}
	 */

	private void validateFoodCategories(Set<String> categories) {
		if (categories == null || categories.isEmpty()) {
			return;
		}

		// DB에 존재하는 카테고리 수
		long validCount = foodCategoryRepository.countByNameIn(categories);

		if (validCount != categories.size()) {
			throw new BusinessException(RestaurantErrorCode.FOOD_CATEGORY_NOT_FOUND);
		}
	}
}
