package com.tasteam.domain.restaurant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.tasteam.domain.restaurant.dto.request.RestaurantCreateRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantUpdateRequest;
import com.tasteam.domain.restaurant.dto.request.ReviewResponse;
import com.tasteam.domain.restaurant.dto.response.*;
import com.tasteam.domain.restaurant.service.RestaurantService;
import com.tasteam.domain.review.service.ReviewService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/restaurants")
public class RestaurantController {

	private final RestaurantService restaurantService;
	private final ReviewService reviewService;

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{restaurantId}")
	public RestaurantDetailResponse getRestaurant(
		@PathVariable
		Long restaurantId) {
		RestaurantDetailResponse.RestaurantDetailData data = restaurantService.getRestaurantDetail(restaurantId);
		return new RestaurantDetailResponse(data);
	}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{restaurantId}/reviews")
	public CursorPageResponse<ReviewResponse> getRestaurantReviews(
		@PathVariable
		Long restaurantId,
		@ModelAttribute
		RestaurantReviewListRequest request) {
		return reviewService.getRestaurantReviews(restaurantId, request);
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping
	public RestaurantCreateResponse createRestaurant(@RequestBody
	RestaurantCreateRequest request) {
		RestaurantCreateResponse.RestaurantCreateData data = restaurantService.createRestaurant(request);
		return new RestaurantCreateResponse(data);
	}

	@ResponseStatus(HttpStatus.OK)
	@PreAuthorize("hasRole('ADMIN')")
	@PatchMapping("/{restaurantId}")
	public RestaurantUpdateResponse updateRestaurant(
		@PathVariable
		Long restaurantId,
		@RequestBody
		RestaurantUpdateRequest request) {
		RestaurantUpdateResponse.RestaurantUpdateData data = restaurantService.updateRestaurant(restaurantId, request);
		return new RestaurantUpdateResponse(data);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{restaurantId}")
	public void deleteRestaurant(@PathVariable
	Long restaurantId) {
		restaurantService.deleteRestaurant(restaurantId);
	}
}
