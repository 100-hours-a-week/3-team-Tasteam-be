package com.tasteam.domain.restaurant.dto.response;

import java.time.Instant;

public record RestaurantCreateResponse(long id, Instant createdAt) {
}
