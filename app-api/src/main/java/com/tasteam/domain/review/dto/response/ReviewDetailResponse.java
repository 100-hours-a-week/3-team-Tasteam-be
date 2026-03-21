package com.tasteam.domain.review.dto.response;

import java.time.Instant;
import java.util.List;

public record ReviewDetailResponse(
	Long id,
	RestaurantResponse restaurant,
	AuthorResponse author,
	String content,
	boolean isRecommended,
	List<String> keywords,
	List<ReviewImageResponse> images,
	Instant createdAt,
	Instant updatedAt) {

	public record RestaurantResponse(Long id, String name) {
	}

	public record AuthorResponse(Long id, String nickname, String profileImageUrl) {
	}

	public record ReviewImageResponse(Long id, String url) {
	}
}
