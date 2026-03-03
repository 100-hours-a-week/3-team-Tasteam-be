package com.tasteam.domain.restaurant.dto.response;

import java.util.List;

/**
 * 카테고리별 비교 분석 상세 (API 응답).
 */
public record AiCategoryComparisonResponse(
	String summary,
	List<String> bullets,
	List<AiEvidenceResponse> evidences,
	Double liftScore) {
}
