package com.tasteam.domain.restaurant.dto.response;

import java.util.List;

import com.tasteam.domain.restaurant.entity.MenuCategory;

public record MenuCategoryResponse(
	Long id,
	String name,
	Integer displayOrder,
	List<MenuItemResponse> menus) {

	public static MenuCategoryResponse of(MenuCategory category, List<MenuItemResponse> menus) {
		return new MenuCategoryResponse(
			category.getId(),
			category.getName(),
			category.getDisplayOrder(),
			menus);
	}
}
