package com.tasteam.domain.recommendation.importer;

import org.springframework.util.StringUtils;

import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;

/**
 * 추천 결과 ingest 실행 입력 정보.
 */
public record RecommendationResultImportRequest(
	String requestedModelVersion,
	String s3Uri,
	String requestId) {

	public RecommendationResultImportRequest {
		if (!StringUtils.hasText(requestedModelVersion)) {
			throw RecommendationBusinessException.resultValidationFailed("요청 모델 버전은 비어 있을 수 없습니다.");
		}
		if (!StringUtils.hasText(s3Uri) || !s3Uri.startsWith("s3://")) {
			throw RecommendationBusinessException.resultValidationFailed("s3Uri는 s3:// 형식이어야 합니다.");
		}
	}
}
