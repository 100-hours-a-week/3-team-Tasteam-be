package com.tasteam.domain.restaurant.dto.response;

import java.util.List;

/**
 * 카테고리별 리뷰 요약 상세 (API 응답).
 */
public record AiCategorySummaryResponse(
	String summary,
	List<String> bullets,
	List<AiEvidenceResponse> evidences) {
}
