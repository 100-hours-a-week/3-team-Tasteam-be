package com.tasteam.domain.restaurant.dto.response;

import java.time.Instant;

public record RestaurantUpdateResponse(long id, Instant createdAt, Instant updatedAt) {
}
