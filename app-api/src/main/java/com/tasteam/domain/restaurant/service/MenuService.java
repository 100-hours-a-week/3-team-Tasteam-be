package com.tasteam.domain.restaurant.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.file.repository.DomainImageRepository;
import com.tasteam.domain.file.repository.ImageRepository;
import com.tasteam.domain.restaurant.dto.request.MenuBulkCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCategoryCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCreateRequest;
import com.tasteam.domain.restaurant.dto.response.MenuCategoryResponse;
import com.tasteam.domain.restaurant.dto.response.MenuItemResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.tasteam.domain.restaurant.entity.Menu;
import com.tasteam.domain.restaurant.entity.MenuCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.MenuCategoryRepository;
import com.tasteam.domain.restaurant.repository.MenuRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.CommonErrorCode;
import com.tasteam.global.exception.code.RestaurantErrorCode;
import com.tasteam.infra.storage.StorageClient;
import com.tasteam.infra.storage.StorageProperties;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class MenuService {

	private final RestaurantRepository restaurantRepository;
	private final MenuCategoryRepository menuCategoryRepository;
	private final MenuRepository menuRepository;
	private final ImageRepository imageRepository;
	private final DomainImageRepository domainImageRepository;
	private final StorageProperties storageProperties;
	private final StorageClient storageClient;

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

	@Transactional
	public Long createMenuCategory(Long restaurantId, MenuCategoryCreateRequest request) {
		Restaurant restaurant = restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

		MenuCategory category = MenuCategory.create(
			restaurant,
			request.name(),
			request.displayOrder());

		return menuCategoryRepository.save(category).getId();
	}

	@Transactional
	public Long createMenu(Long restaurantId, MenuCreateRequest request) {
		restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

		MenuCategory category = menuCategoryRepository.findById(request.categoryId())
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.MENU_CATEGORY_NOT_FOUND));

		if (!category.getRestaurant().getId().equals(restaurantId)) {
			throw new BusinessException(RestaurantErrorCode.MENU_CATEGORY_NOT_FOUND);
		}

		Menu menu = Menu.create(
			category,
			request.name(),
			request.description(),
			request.price(),
			request.imageUrl(),
			request.isRecommended(),
			request.displayOrder());

		Menu saved = menuRepository.save(menu);
		applyMenuImage(saved, request.imageFileUuid(), request.imageUrl());
		return saved.getId();
	}

	@Transactional
	public List<Long> createMenusBulk(Long restaurantId, MenuBulkCreateRequest request) {
		restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)
			.orElseThrow(() -> new BusinessException(RestaurantErrorCode.RESTAURANT_NOT_FOUND));

		Set<Long> categoryIds = request.menus().stream()
			.map(MenuCreateRequest::categoryId)
			.collect(Collectors.toSet());

		Map<Long, MenuCategory> categoryMap = menuCategoryRepository.findAllById(categoryIds).stream()
			.collect(Collectors.toMap(MenuCategory::getId, c -> c));

		for (Long categoryId : categoryIds) {
			MenuCategory category = categoryMap.get(categoryId);
			if (category == null || !category.getRestaurant().getId().equals(restaurantId)) {
				throw new BusinessException(RestaurantErrorCode.MENU_CATEGORY_NOT_FOUND);
			}
		}

		List<Menu> menus = request.menus().stream()
			.map(menuRequest -> Menu.create(
				categoryMap.get(menuRequest.categoryId()),
				menuRequest.name(),
				menuRequest.description(),
				menuRequest.price(),
				menuRequest.imageUrl(),
				menuRequest.isRecommended(),
				menuRequest.displayOrder()))
			.toList();

		List<Menu> savedMenus = menuRepository.saveAll(menus);

		for (int i = 0; i < savedMenus.size(); i++) {
			MenuCreateRequest menuRequest = request.menus().get(i);
			applyMenuImage(savedMenus.get(i), menuRequest.imageFileUuid(), menuRequest.imageUrl());
		}

		return savedMenus.stream().map(Menu::getId).toList();
	}

	private void applyMenuImage(Menu menu, String imageFileUuid, String fallbackImageUrl) {
		if (fallbackImageUrl != null && !fallbackImageUrl.isBlank()) {
			menu.changeImageUrl(fallbackImageUrl);
		}
		// 임시로 imageFileUuid 기반 연결 로직은 비활성화한다.
	}

	private UUID parseUuid(String fileUuid) {
		try {
			return UUID.fromString(fileUuid);
		} catch (IllegalArgumentException ex) {
			throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "fileUuid 형식이 올바르지 않습니다");
		}
	}

	private String buildPublicUrl(String storageKey) {
		if (storageProperties.isPresignedAccess()) {
			return storageClient.createPresignedGetUrl(storageKey);
		}
		String baseUrl = storageProperties.getBaseUrl();
		if (baseUrl == null || baseUrl.isBlank()) {
			baseUrl = String.format("https://%s.s3.%s.amazonaws.com",
				storageProperties.getBucket(),
				storageProperties.getRegion());
		}
		String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		String normalizedKey = storageKey.startsWith("/") ? storageKey.substring(1) : storageKey;
		return normalizedBase + "/" + normalizedKey;
	}
}
