package com.tasteam.domain.admin.dto.request;

public record AdminRestaurantSearchCondition(
	String name,
	String address,
	Long foodCategoryId,
	Boolean isDeleted) {
}
