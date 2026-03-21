package com.tasteam.domain.restaurant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.tasteam.domain.restaurant.controller.docs.RestaurantControllerDocs;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.*;
import com.tasteam.domain.restaurant.service.RestaurantService;
import com.tasteam.domain.review.dto.request.ReviewCreateRequest;
import com.tasteam.domain.review.dto.response.ReviewCreateResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController implements RestaurantControllerDocs {

	private final RestaurantService restaurantService;
	private final ReviewService reviewService;

	@GetMapping
	public void getRestaurants() {
		throw new BusinessException(RestaurantErrorCode.RESTAURANT_LIST_ENDPOINT_DISABLED);
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{restaurantId}")
	public SuccessResponse<RestaurantDetailResponse> getRestaurant(
		@PathVariable
		Long restaurantId) {
		return SuccessResponse.success(restaurantService.getRestaurantDetail(restaurantId));
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{restaurantId}/reviews")
	public SuccessResponse<CursorPageResponse<ReviewResponse>> getRestaurantReviews(
		@PathVariable
		Long restaurantId,
		@ModelAttribute
		RestaurantReviewListRequest request) {
		return SuccessResponse.success(reviewService.getRestaurantReviews(restaurantId, request));
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('USER')")
	@PostMapping("/{restaurantId}/reviews")
	public SuccessResponse<ReviewCreateResponse> createReview(
		@PathVariable
		Long restaurantId,
		@CurrentUser
		Long memberId,
		@RequestBody @Validated
		ReviewCreateRequest request) {
		return SuccessResponse.success(reviewService.createReview(memberId, restaurantId, request));
	}
}
