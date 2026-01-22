package com.tasteam.domain.search.dto;

import java.time.Instant;

public record SearchCursor(Instant updatedAt, Long id) {
}
