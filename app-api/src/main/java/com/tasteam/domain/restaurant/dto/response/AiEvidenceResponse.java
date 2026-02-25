package com.tasteam.domain.restaurant.dto.response;

/**
 * 근거 리뷰 1건 (API 응답).
 */
public record AiEvidenceResponse(
	Long reviewId,
	String snippet) {
}
