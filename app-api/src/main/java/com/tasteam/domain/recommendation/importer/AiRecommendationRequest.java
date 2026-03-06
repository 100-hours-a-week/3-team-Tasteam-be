package com.tasteam.domain.recommendation.importer;

import org.springframework.util.StringUtils;

import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;

/**
 * AI 추천 요청 입력.
 */
public record AiRecommendationRequest(
	String modelVersion,
	String requestId) {

	public AiRecommendationRequest {
		if (!StringUtils.hasText(modelVersion)) {
			throw RecommendationBusinessException.resultValidationFailed("AI 요청 모델 버전은 비어 있을 수 없습니다.");
		}
	}
}
