package com.tasteam.domain.restaurant.dto.response;

import com.tasteam.domain.restaurant.entity.Menu;

public record MenuItemResponse(
	Long id,
	Long restaurantId,
	String name,
	String description,
	Integer price,
	String imageUrl,
	Boolean isRecommended,
	Integer displayOrder) {

	public static MenuItemResponse from(Menu menu) {
		return new MenuItemResponse(
			menu.getId(),
			menu.getCategory() == null || menu.getCategory().getRestaurant() == null
				? null
				: menu.getCategory().getRestaurant().getId(),
			menu.getName(),
			menu.getDescription(),
			menu.getPrice(),
			menu.getImageUrl(),
			menu.getIsRecommended(),
			menu.getDisplayOrder());
	}
}
