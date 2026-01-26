package com.tasteam.domain.search.dto.response;

public record SearchRestaurantItem(
	long restaurantId,
	String name,
	String address,
	String imageUrl) {
}
