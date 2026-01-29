package com.tasteam.domain.restaurant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.tasteam.domain.restaurant.controller.docs.RestaurantControllerDocs;
import com.tasteam.domain.restaurant.dto.request.NearbyRestaurantQueryParams;
import com.tasteam.domain.restaurant.dto.request.RestaurantCreateRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantUpdateRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.*;
import com.tasteam.domain.restaurant.service.RestaurantService;
import com.tasteam.domain.review.dto.request.ReviewCreateRequest;
import com.tasteam.domain.review.dto.response.ReviewCreateResponse;
import com.tasteam.domain.review.service.ReviewService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController implements RestaurantControllerDocs {

	private final RestaurantService restaurantService;
	private final ReviewService reviewService;

	@ResponseStatus(HttpStatus.OK)
	@GetMapping
	public SuccessResponse<CursorPageResponse<RestaurantListItem>> getRestaurants(
		@ModelAttribute @Validated
		NearbyRestaurantQueryParams queryParams) {

		return SuccessResponse.success(restaurantService.getRestaurants(queryParams));
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{restaurantId}")
	public SuccessResponse<RestaurantDetailResponse> getRestaurant(
		@PathVariable
		Long restaurantId) {
		return SuccessResponse.success(restaurantService.getRestaurantDetail(restaurantId));
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public SuccessResponse<RestaurantCreateResponse> createRestaurant(@RequestBody
	RestaurantCreateRequest request) {
		return SuccessResponse.success(restaurantService.createRestaurant(request));
	}

	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/{restaurantId}")
	public SuccessResponse<RestaurantUpdateResponse> updateRestaurant(
		@PathVariable
		Long restaurantId,
		@RequestBody
		RestaurantUpdateRequest request) {
		return SuccessResponse.success(restaurantService.updateRestaurant(restaurantId, request));
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{restaurantId}")
	public void deleteRestaurant(@PathVariable
	Long restaurantId) {
		restaurantService.deleteRestaurant(restaurantId);
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
