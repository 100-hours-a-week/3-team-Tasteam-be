package com.tasteam.fixture;

import java.util.List;
import java.util.UUID;

import com.tasteam.domain.review.dto.request.ReviewCreateRequest;

public final class ReviewRequestFixture {

	public static final String DEFAULT_CONTENT = "테스트 리뷰 내용";

	private ReviewRequestFixture() {}

	public static ReviewCreateRequest createRequest(Long groupId, List<Long> keywordIds) {
		return new ReviewCreateRequest(groupId, null, DEFAULT_CONTENT, true, keywordIds, null);
	}

	public static ReviewCreateRequest createRequest(Long groupId, List<Long> keywordIds, List<UUID> imageIds) {
		return new ReviewCreateRequest(groupId, null, DEFAULT_CONTENT, true, keywordIds, imageIds);
	}

	public static ReviewCreateRequest createRequest(Long groupId, List<Long> keywordIds, String content,
		boolean isRecommended, List<UUID> imageIds) {
		return new ReviewCreateRequest(groupId, null, content, isRecommended, keywordIds, imageIds);
	}
}
