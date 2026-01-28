package com.tasteam.domain.restaurant.dto.response;

import com.tasteam.domain.restaurant.entity.Menu;

public record MenuItemResponse(
	Long id,
	String name,
	String description,
	Integer price,
	String imageUrl,
	Boolean isRecommended,
	Integer displayOrder) {

	public static MenuItemResponse from(Menu menu) {
		return new MenuItemResponse(
			menu.getId(),
			menu.getName(),
			menu.getDescription(),
			menu.getPrice(),
			menu.getImageUrl(),
			menu.getIsRecommended(),
			menu.getDisplayOrder());
	}
}
