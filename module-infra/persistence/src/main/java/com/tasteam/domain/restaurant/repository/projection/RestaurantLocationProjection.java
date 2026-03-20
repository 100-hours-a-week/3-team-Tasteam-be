package com.tasteam.domain.restaurant.repository.projection;

public interface RestaurantLocationProjection {
	Long getId();

	String getName();

	Double getLatitude();

	Double getLongitude();
}
