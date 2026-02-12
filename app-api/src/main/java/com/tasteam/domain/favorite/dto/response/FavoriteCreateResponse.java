package com.tasteam.domain.favorite.dto.response;

import java.time.Instant;

public record FavoriteCreateResponse(
	Long id,
	Long restaurantId,
	Instant createdAt) {
}
