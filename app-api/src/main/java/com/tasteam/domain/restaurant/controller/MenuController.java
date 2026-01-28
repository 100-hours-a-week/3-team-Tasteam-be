package com.tasteam.domain.restaurant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.restaurant.controller.docs.MenuControllerDocs;
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
}
