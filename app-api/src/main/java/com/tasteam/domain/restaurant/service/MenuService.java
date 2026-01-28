package com.tasteam.domain.restaurant.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.dto.response.MenuCategoryResponse;
import com.tasteam.domain.restaurant.dto.response.MenuItemResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.tasteam.domain.restaurant.entity.Menu;
import com.tasteam.domain.restaurant.entity.MenuCategory;
import com.tasteam.domain.restaurant.repository.MenuCategoryRepository;
import com.tasteam.domain.restaurant.repository.MenuRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MenuService {

	private final RestaurantRepository restaurantRepository;
	private final MenuCategoryRepository menuCategoryRepository;
	private final MenuRepository menuRepository;

	@Transactional(readOnly = true)
	public RestaurantMenuResponse getRestaurantMenus(
		Long restaurantId,
		boolean includeEmptyCategories,
		boolean recommendedFirst) {

		restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

		List<MenuCategory> categories = menuCategoryRepository.findByRestaurantIdOrderByDisplayOrder(restaurantId);

		if (categories.isEmpty()) {
			return new RestaurantMenuResponse(restaurantId, List.of());
		}

		Set<Long> categoryIds = categories.stream()
			.map(MenuCategory::getId)
			.collect(Collectors.toSet());

		List<Menu> menus = recommendedFirst
			? menuRepository.findByCategoryIdsOrderByRecommendedAndDisplayOrder(categoryIds)
			: menuRepository.findByCategoryIdsOrderByDisplayOrder(categoryIds);

		Map<Long, List<Menu>> menusByCategory = menus.stream()
			.collect(Collectors.groupingBy(menu -> menu.getCategory().getId()));

		List<MenuCategoryResponse> categoryResponses = new ArrayList<>();

		for (MenuCategory category : categories) {
			List<Menu> categoryMenus = menusByCategory.getOrDefault(category.getId(), List.of());

			if (!includeEmptyCategories && categoryMenus.isEmpty()) {
				continue;
			}

			List<MenuItemResponse> menuItems = categoryMenus.stream()
				.map(MenuItemResponse::from)
				.toList();

			categoryResponses.add(MenuCategoryResponse.of(category, menuItems));
		}

		return new RestaurantMenuResponse(restaurantId, categoryResponses);
	}
}
