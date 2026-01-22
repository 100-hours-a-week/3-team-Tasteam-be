package com.tasteam.domain.search.dto;

import java.time.Instant;

public record RecentSearchCursor(Instant updatedAt, Long id) {
}
