package com.tasteam.domain.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.dto.request.AdminFoodCategoryCreateRequest;
import com.tasteam.domain.restaurant.service.FoodCategoryService;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/food-categories")
public class AdminFoodCategoryController {

	private final FoodCategoryService foodCategoryService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<Long> createFoodCategory(
		@Validated @RequestBody
		AdminFoodCategoryCreateRequest request) {

		Long categoryId = foodCategoryService.createFoodCategory(request.name());
		return SuccessResponse.success(categoryId);
	}
}
