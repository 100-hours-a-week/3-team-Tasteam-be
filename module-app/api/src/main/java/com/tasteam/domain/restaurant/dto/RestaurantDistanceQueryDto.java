package com.tasteam.domain.restaurant.dto;

public record RestaurantDistanceQueryDto(
	long id,
	String name,
	String address,
	double distanceMeter) {
}
