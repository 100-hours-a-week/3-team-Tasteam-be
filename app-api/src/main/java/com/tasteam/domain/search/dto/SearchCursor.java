package com.tasteam.domain.search.dto;

import java.time.Instant;

public record SearchCursor(
	Integer nameExact,
	Double nameSimilarity,
	Double distanceMeters,
	Integer categoryMatch,
	Integer addressMatch,
	Instant updatedAt,
	Long id) {
}
