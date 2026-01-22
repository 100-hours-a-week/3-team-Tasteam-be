package com.tasteam.domain.restaurant.dto.request;

import java.time.Instant;
import java.util.List;

public record ReviewResponse(
	long id,
	AuthorResponse author,
	String contentPreview,
	boolean isRecommended,
	List<String> keywords,
	ReviewImageResponse thumbnailImage,
	Instant createdAt) {
	public record AuthorResponse(String nickname) {
	}

	public record ReviewImageResponse(long id, String url) {
	}
}
