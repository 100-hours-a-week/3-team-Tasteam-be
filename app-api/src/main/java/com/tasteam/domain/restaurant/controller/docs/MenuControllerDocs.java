package com.tasteam.domain.restaurant.controller.docs;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.error.code.restaurant.RestaurantSwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Restaurant", description = "음식점 메뉴 API")
public interface MenuControllerDocs {

	@Operation(summary = "음식점 메뉴 목록 조회", description = "음식점 메뉴를 카테고리별로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = RestaurantMenuResponse.class)))
	@CustomErrorResponseDescription(value = RestaurantSwaggerErrorResponseDescription.class, group = "MENU_LIST")
	SuccessResponse<RestaurantMenuResponse> getRestaurantMenus(
		@Parameter(description = "음식점 ID", example = "1001") @PathVariable
		Long restaurantId,
		@Parameter(description = "비어있는 카테고리 포함 여부", example = "false") @RequestParam(defaultValue = "false")
		boolean includeEmptyCategories,
		@Parameter(description = "추천 메뉴 우선 정렬 여부", example = "true") @RequestParam(defaultValue = "true")
		boolean recommendedFirst);
}
