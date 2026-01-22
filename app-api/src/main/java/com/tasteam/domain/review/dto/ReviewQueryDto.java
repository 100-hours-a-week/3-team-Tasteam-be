package com.tasteam.domain.review.dto;

import java.time.Instant;

public record ReviewQueryDto(
	Long reviewId,
	Long memberId,
	String memberName,
	String content,
	boolean isRecommended,
	Instant createdAt) {
}
