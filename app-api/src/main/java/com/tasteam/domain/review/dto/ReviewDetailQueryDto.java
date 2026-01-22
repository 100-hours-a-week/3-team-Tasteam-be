package com.tasteam.domain.review.dto;

import java.time.Instant;

public record ReviewDetailQueryDto(
	Long reviewId,
	Long restaurantId,
	String restaurantName,
	Long memberId,
	String memberNickname,
	String content,
	boolean isRecommended,
	Instant createdAt,
	Instant updatedAt) {
}
