package com.tasteam.domain.restaurant.controller.docs;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.tasteam.global.dto.api.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Restaurant", description = "음식점 메뉴 API")
public interface MenuControllerDocs {

	@Operation(summary = "음식점 메뉴 목록 조회", description = "음식점 메뉴를 카테고리별로 조회합니다.")
	SuccessResponse<RestaurantMenuResponse> getRestaurantMenus(
		@PathVariable
		Long restaurantId,
		@RequestParam(defaultValue = "false")
		boolean includeEmptyCategories,
		@RequestParam(defaultValue = "true")
		boolean recommendedFirst);
}
