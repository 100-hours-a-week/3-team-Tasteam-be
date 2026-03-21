package com.tasteam.domain.review.dto;

import java.time.Instant;

public record ReviewMemberQueryDto(
	Long reviewId,
	String restaurantName,
	String restaurantAddress,
	String content,
	Instant createdAt) {
}
