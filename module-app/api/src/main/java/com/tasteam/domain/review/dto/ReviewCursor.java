package com.tasteam.domain.review.dto;

import java.time.Instant;

public record ReviewCursor(Instant createdAt, long id) {
}
