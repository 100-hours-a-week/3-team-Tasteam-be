package com.tasteam.domain.restaurant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.tasteam.domain.restaurant.dto.request.NearbyRestaurantQueryParams;
import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantListItem;
import com.tasteam.domain.restaurant.service.RestaurantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/groups/{groupId}/reviews")
public class GroupReviewController {

	private final RestaurantService restaurantService;

	// TODO: 그룹 리뷰 목록 페이지 조회 API 구현
	@ResponseStatus(HttpStatus.OK)
	@GetMapping
	public void getGroupReviews() {}

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/restaurants")
	public CursorPageResponse<RestaurantListItem> getGroupReviewRestaurants(
		@PathVariable
		Long groupId,
		@Valid @ModelAttribute
		NearbyRestaurantQueryParams queryParams) {
		return restaurantService.getGroupRestaurants(groupId, queryParams);
	}
}
