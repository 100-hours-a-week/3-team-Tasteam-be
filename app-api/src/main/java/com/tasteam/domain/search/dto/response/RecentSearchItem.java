package com.tasteam.domain.search.dto.response;

import java.time.Instant;

public record RecentSearchItem(
	long id,
	String keyword,
	Instant updatedAt) {
}
