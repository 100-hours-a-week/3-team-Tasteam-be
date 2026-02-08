package com.tasteam.domain.restaurant.controller.docs;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;

import com.tasteam.domain.restaurant.dto.request.NearbyRestaurantQueryParams;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantListItem;
import com.tasteam.domain.review.dto.request.ReviewCreateRequest;
import com.tasteam.domain.review.dto.response.ReviewCreateResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.error.code.restaurant.RestaurantSwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Restaurant", description = "음식점 조회/관리 API")
public interface RestaurantControllerDocs {

	@Operation(summary = "음식점 목록 조회", description = "지정한 위치 조건으로 주변 음식점을 커서 기반으로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	SuccessResponse<CursorPageResponse<RestaurantListItem>> getRestaurants(
		@Validated @ParameterObject
		NearbyRestaurantQueryParams queryParams);

	@Operation(summary = "음식점 상세 조회", description = "음식점 상세 정보를 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = RestaurantDetailResponse.class)))
	@CustomErrorResponseDescription(value = RestaurantSwaggerErrorResponseDescription.class, group = "RESTAURANT_DETAIL")
	SuccessResponse<RestaurantDetailResponse> getRestaurant(
		@Parameter(description = "음식점 ID", example = "1001") @PathVariable
		Long restaurantId);

	@Operation(summary = "음식점 리뷰 목록 조회", description = "음식점 리뷰를 커서 기반으로 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	@CustomErrorResponseDescription(value = RestaurantSwaggerErrorResponseDescription.class, group = "RESTAURANT_REVIEWS")
	SuccessResponse<CursorPageResponse<ReviewResponse>> getRestaurantReviews(
		@Parameter(description = "음식점 ID", example = "1001") @PathVariable
		Long restaurantId,
		@ParameterObject
		RestaurantReviewListRequest request);

	@Operation(summary = "리뷰 등록", description = "사용자 권한으로 음식점 리뷰를 작성합니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = ReviewCreateRequest.class)))
	@ApiResponse(responseCode = "201", description = "리뷰 등록 완료", content = @Content(schema = @Schema(implementation = ReviewCreateResponse.class)))
	@CustomErrorResponseDescription(value = RestaurantSwaggerErrorResponseDescription.class, group = "RESTAURANT_REVIEW_CREATE")
	SuccessResponse<ReviewCreateResponse> createReview(
		@Parameter(description = "음식점 ID", example = "1001") @PathVariable
		Long restaurantId,
		@CurrentUser
		Long memberId,
		@Validated
		ReviewCreateRequest request);
}
