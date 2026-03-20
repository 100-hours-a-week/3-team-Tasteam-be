package com.tasteam.domain.restaurant.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.entity.FoodCategory;
import com.tasteam.domain.restaurant.repository.FoodCategoryRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class FoodCategoryService {

	private final FoodCategoryRepository foodCategoryRepository;

	@Transactional
	public Long createFoodCategory(String name) {
		return foodCategoryRepository.save(FoodCategory.create(name)).getId();
	}
}
