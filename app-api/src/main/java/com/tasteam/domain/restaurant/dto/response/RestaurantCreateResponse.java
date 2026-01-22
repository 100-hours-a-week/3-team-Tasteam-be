package com.tasteam.domain.restaurant.dto.response;

import java.time.Instant;

public record RestaurantCreateResponse(RestaurantCreateData data) {

	public record RestaurantCreateData(long id, Instant createdAt) {
	}
}
