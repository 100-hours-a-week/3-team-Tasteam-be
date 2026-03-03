package com.tasteam.domain.admin.dto.response;

import java.time.Instant;

public record AdminReviewListItem(
	Long id,
	Long restaurantId,
	String restaurantName,
	Long memberId,
	String memberNickname,
	String content,
	boolean isRecommended,
	Instant createdAt) {
}
