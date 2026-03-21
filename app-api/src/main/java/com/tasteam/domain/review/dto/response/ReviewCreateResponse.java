package com.tasteam.domain.review.dto.response;

import java.time.Instant;

public record ReviewCreateResponse(long id, Instant createdAt) {
}
