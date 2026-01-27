package com.tasteam.domain.review.dto.response;

import com.tasteam.domain.review.entity.Keyword;
import com.tasteam.domain.review.entity.KeywordType;

public record ReviewKeywordItemResponse(
	Long id,
	KeywordType type,
	String name) {

	public static ReviewKeywordItemResponse from(Keyword keyword) {
		return new ReviewKeywordItemResponse(
			keyword.getId(),
			keyword.getType(),
			keyword.getName());
	}
}
