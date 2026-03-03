package com.tasteam.domain.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.admin.controller.docs.AdminRestaurantControllerDocs;
import com.tasteam.domain.admin.dto.request.AdminRestaurantCreateRequest;
import com.tasteam.domain.admin.dto.request.AdminRestaurantSearchCondition;
import com.tasteam.domain.admin.dto.request.AdminRestaurantUpdateRequest;
import com.tasteam.domain.admin.dto.response.AdminRestaurantDetailResponse;
import com.tasteam.domain.admin.dto.response.AdminRestaurantListItem;
import com.tasteam.domain.admin.service.AdminRestaurantService;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/restaurants")
public class AdminRestaurantController implements AdminRestaurantControllerDocs {

	private final AdminRestaurantService adminRestaurantService;

	@Override
	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<Page<AdminRestaurantListItem>> getRestaurants(
		@ModelAttribute
		AdminRestaurantSearchCondition condition,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
		Pageable pageable) {

		Page<AdminRestaurantListItem> result = adminRestaurantService.getRestaurants(condition, pageable);
		return SuccessResponse.success(result);
	}

	@Override
	@GetMapping("/{restaurantId}")
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<AdminRestaurantDetailResponse> getRestaurant(
		@PathVariable
		Long restaurantId) {

		AdminRestaurantDetailResponse result = adminRestaurantService.getRestaurantDetail(restaurantId);
		return SuccessResponse.success(result);
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SuccessResponse<Long> createRestaurant(
		@Validated @RequestBody
		AdminRestaurantCreateRequest request) {

		Long restaurantId = adminRestaurantService.createRestaurant(request);
		return SuccessResponse.success(restaurantId);
	}

	@Override
	@PatchMapping("/{restaurantId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void updateRestaurant(
		@PathVariable
		Long restaurantId,
		@Validated @RequestBody
		AdminRestaurantUpdateRequest request) {

		adminRestaurantService.updateRestaurant(restaurantId, request);
	}

	@Override
	@DeleteMapping("/{restaurantId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteRestaurant(
		@PathVariable
		Long restaurantId) {

		adminRestaurantService.deleteRestaurant(restaurantId);
	}
}
