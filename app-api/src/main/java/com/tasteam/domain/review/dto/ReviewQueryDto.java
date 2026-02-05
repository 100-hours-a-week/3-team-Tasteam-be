package com.tasteam.domain.review.dto;

import java.time.Instant;

public record ReviewQueryDto(
	Long reviewId,
	Long groupId,
	Long subgroupId,
	String groupName,
	String subgroupName,
	Long memberId,
	String memberName,
	String content,
	boolean isRecommended,
	Instant createdAt) {
}
