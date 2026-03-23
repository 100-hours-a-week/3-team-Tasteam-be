package com.tasteam.domain.restaurant.controller.docs;

import java.util.List;

import com.tasteam.domain.restaurant.dto.response.FoodCategoryResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(30)
@Tag(name = "Restaurant", description = "음식점 조회/관리 API")
public interface FoodCategoryControllerDocs {

	@Operation(summary = "음식 카테고리 목록 조회", description = "전체 음식 카테고리 목록을 조회합니다.")
	SuccessResponse<List<FoodCategoryResponse>> getFoodCategories();
}
