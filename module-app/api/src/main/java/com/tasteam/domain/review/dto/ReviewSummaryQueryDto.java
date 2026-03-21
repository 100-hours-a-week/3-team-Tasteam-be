package com.tasteam.domain.review.dto;

import java.time.Instant;
import java.util.List;

public record ReviewSummaryQueryDto(
	long id,
	long memberId,
	String memberNickname,
	String content,
	boolean isRecommended,
	List<String> keywords,
	ReviewImageQueryDto thumbnailImage,
	Instant createdAt) {

	public record ReviewImageQueryDto(long id, String url) {
	}
}
