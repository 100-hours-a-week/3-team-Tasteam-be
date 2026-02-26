package com.tasteam.domain.restaurant.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.restaurant.controller.docs.FoodCategoryControllerDocs;
import com.tasteam.domain.restaurant.dto.response.FoodCategoryResponse;
import com.tasteam.domain.restaurant.service.FoodCategoryService;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/food-categories")
public class FoodCategoryController implements FoodCategoryControllerDocs {

	private final FoodCategoryService foodCategoryService;

	@Override
	@ResponseStatus(HttpStatus.OK)
	@GetMapping
	public SuccessResponse<List<FoodCategoryResponse>> getFoodCategories() {
		return SuccessResponse.success(foodCategoryService.getFoodCategories());
	}
}
