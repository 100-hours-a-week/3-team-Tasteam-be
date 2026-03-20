package com.tasteam.domain.admin.controller.docs;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.domain.restaurant.dto.request.MenuBulkCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCategoryCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCreateRequest;
import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(110)
@Tag(name = "Admin - Restaurant", description = "어드민 음식점 관리 API")
public interface AdminMenuControllerDocs {

	@Operation(summary = "메뉴 목록 조회", description = "음식점의 메뉴 목록을 카테고리별로 조회합니다.")
	SuccessResponse<RestaurantMenuResponse> getMenus(
		@Parameter(description = "음식점 ID", example = "1") @PathVariable
		Long restaurantId,
		@Parameter(description = "빈 카테고리 포함 여부") @RequestParam
		boolean includeEmptyCategories,
		@Parameter(description = "추천 메뉴 우선 정렬") @RequestParam
		boolean recommendedFirst);

	@Operation(summary = "메뉴 카테고리 등록", description = "음식점에 메뉴 카테고리를 등록합니다.")
	SuccessResponse<Long> createMenuCategory(
		@Parameter(description = "음식점 ID", example = "1") @PathVariable
		Long restaurantId,
		@Validated @RequestBody
		MenuCategoryCreateRequest request);

	@Operation(summary = "메뉴 등록", description = "음식점에 메뉴를 등록합니다.")
	SuccessResponse<Long> createMenu(
		@Parameter(description = "음식점 ID", example = "1") @PathVariable
		Long restaurantId,
		@Validated @RequestBody
		MenuCreateRequest request);

	@Operation(summary = "메뉴 일괄 등록", description = "음식점에 메뉴를 일괄 등록합니다.")
	SuccessResponse<Void> createMenusBulk(
		@Parameter(description = "음식점 ID", example = "1") @PathVariable
		Long restaurantId,
		@Validated @RequestBody
		MenuBulkCreateRequest request);
}
