package com.tasteam.domain.recommendation.importer;

import org.springframework.util.StringUtils;

import com.tasteam.domain.recommendation.exception.RecommendationBusinessException;

/**
 * 추천 결과 import 오케스트레이션 입력 모델.
 */
public record RecommendationResultImportFacadeCommand(
	String requestedModelVersion,
	String resultS3Uri,
	String requestId) {

	public RecommendationResultImportFacadeCommand {
		if (!StringUtils.hasText(requestedModelVersion)) {
			throw RecommendationBusinessException.resultValidationFailed("요청 모델 버전은 비어 있을 수 없습니다.");
		}
		if (!StringUtils.hasText(resultS3Uri) || !resultS3Uri.startsWith("s3://")) {
			throw RecommendationBusinessException.resultValidationFailed("resultS3Uri는 s3:// 형식이어야 합니다.");
		}
	}
}
