package com.tasteam.domain.review.dto.response;

import java.time.Instant;

public record ReviewCreateResponse(ReviewCreateData data) {

	public record ReviewCreateData(long id, Instant createdAt) {
	}
}
