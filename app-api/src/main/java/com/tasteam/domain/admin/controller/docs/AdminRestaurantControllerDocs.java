package com.tasteam.domain.admin.controller.docs;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.tasteam.domain.admin.dto.request.AdminRestaurantCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminRestaurantSearchCondition;
import com.tasteam.domain.admin.dto.request.AdminRestaurantUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminRestaurantDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminRestaurantListItem;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(110)
@Tag(name = "Admin - Restaurant", description = "어드민 음식점 관리 API")
public interface AdminRestaurantControllerDocs {

	@Operation(summary = "음식점 목록 조회", description = "검색 조건에 따라 음식점 목록을 페이지네이션으로 조회합니다.")
	SuccessResponse<Page<AdminRestaurantListItem>> getRestaurants(
		AdminRestaurantSearchCondition condition,
		Pageable pageable);

	@Operation(summary = "음식점 상세 조회", description = "음식점 ID로 상세 정보를 조회합니다.")
	SuccessResponse<AdminRestaurantDetailResponse> getRestaurant(
		@Parameter(description = "음식점 ID", example = "1") @PathVariable
		Long restaurantId);

	@Operation(summary = "음식점 등록", description = "새 음식점을 등록합니다.")
	SuccessResponse<Long> createRestaurant(
		@Validated @RequestBody
		AdminRestaurantCreateRequest request);

	@Operation(summary = "음식점 정보 수정", description = "음식점 정보를 수정합니다.")
	void updateRestaurant(
		@Parameter(description = "음식점 ID", example = "1") @PathVariable
		Long restaurantId,
		@Validated @RequestBody
		AdminRestaurantUpdateRequest request);

	@Operation(summary = "음식점 삭제", description = "음식점을 삭제합니다.")
	void deleteRestaurant(
		@Parameter(description = "음식점 ID", example = "1") @PathVariable
		Long restaurantId);
}
