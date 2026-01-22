package com.tasteam.domain.restaurant.dto.response;

import java.time.Instant;

public record RestaurantUpdateResponse(RestaurantUpdateData data) {

	public record RestaurantUpdateData(long id, Instant createdAt, Instant updatedAt) {
	}
}
