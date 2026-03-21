package com.tasteam.domain.favorite.dto;

import java.time.Instant;

public record FavoriteCursor(Instant createdAt, Long id) {
}
