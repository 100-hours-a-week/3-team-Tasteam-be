package com.tasteam.domain.restaurant.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.restaurant.controller.docs.MenuControllerDocs;
import com.tasteam.domain.restaurant.dto.request.MenuBulkCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCategoryCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCreateRequest;
import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.tasteam.domain.restaurant.service.MenuService;
import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/restaurants")
public class MenuController implements MenuControllerDocs {

	private final MenuService menuService;

	@ResponseStatus(HttpStatus.OK)
	@GetMapping("/{restaurantId}/menus")
	public SuccessResponse<RestaurantMenuResponse> getRestaurantMenus(
		@PathVariable
		Long restaurantId,
		@RequestParam(defaultValue = "false")
		boolean includeEmptyCategories,
		@RequestParam(defaultValue = "true")
		boolean recommendedFirst) {
		return SuccessResponse.success(
			menuService.getRestaurantMenus(restaurantId, includeEmptyCategories, recommendedFirst));
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/{restaurantId}/menu-categories")
	public SuccessResponse<Long> createMenuCategory(
		@PathVariable
		Long restaurantId,
		@RequestBody
		MenuCategoryCreateRequest request) {
		return SuccessResponse.success(menuService.createMenuCategory(restaurantId, request));
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/{restaurantId}/menus")
	public SuccessResponse<Long> createMenu(
		@PathVariable
		Long restaurantId,
		@RequestBody
		MenuCreateRequest request) {
		return SuccessResponse.success(menuService.createMenu(restaurantId, request));
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/{restaurantId}/menus/bulk")
	public SuccessResponse<List<Long>> createMenusBulk(
		@PathVariable
		Long restaurantId,
		@RequestBody
		MenuBulkCreateRequest request) {
		return SuccessResponse.success(menuService.createMenusBulk(restaurantId, request));
	}
}
