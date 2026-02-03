package com.tasteam.domain.restaurant.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.dto.response.FoodCategoryResponse;
import com.tasteam.domain.restaurant.entity.FoodCategory;
import com.tasteam.domain.restaurant.repository.FoodCategoryRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class FoodCategoryService {

	private final FoodCategoryRepository foodCategoryRepository;

	@Transactional(readOnly = true)
	public List<FoodCategoryResponse> getFoodCategories() {
		return foodCategoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
			.stream()
			.map(category -> new FoodCategoryResponse(category.getId(), category.getName()))
			.toList();
	}

	@Transactional
	public Long createFoodCategory(String name) {
		return foodCategoryRepository.save(FoodCategory.create(name)).getId();
	}
}
