package com.tasteam.domain.search.dto;

import java.time.Instant;

public record RestaurantSearchRow(Long id, String name, String fullAddress, Instant updatedAt) {
}
