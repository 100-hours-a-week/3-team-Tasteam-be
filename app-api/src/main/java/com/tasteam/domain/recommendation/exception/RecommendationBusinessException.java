package com.tasteam.domain.recommendation.exception;

import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RecommendationErrorCode;

public class RecommendationBusinessException extends BusinessException {

	public RecommendationBusinessException(RecommendationErrorCode errorCode) {
		super(errorCode);
	}

	public RecommendationBusinessException(RecommendationErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public static RecommendationBusinessException modelNotFound(String version) {
		return new RecommendationBusinessException(
			RecommendationErrorCode.RECOMMENDATION_MODEL_NOT_FOUND,
			"추천 모델 정보를 찾을 수 없습니다. version=" + version);
	}

	public static RecommendationBusinessException activeModelNotFound() {
		return new RecommendationBusinessException(
			RecommendationErrorCode.RECOMMENDATION_ACTIVE_MODEL_NOT_FOUND);
	}

	public static RecommendationBusinessException pipelineVersionMismatch(String expected, String actual) {
		return new RecommendationBusinessException(
			RecommendationErrorCode.RECOMMENDATION_PIPELINE_VERSION_MISMATCH,
			"모델 버전이 일치하지 않습니다. expected=" + expected + ", actual=" + actual);
	}

	public static RecommendationBusinessException csvFormatInvalid(String detail) {
		return new RecommendationBusinessException(
			RecommendationErrorCode.RECOMMENDATION_CSV_FORMAT_INVALID,
			detail == null ? RecommendationErrorCode.RECOMMENDATION_CSV_FORMAT_INVALID.getMessage() : detail);
	}

	public static RecommendationBusinessException resultIoError(String detail) {
		return new RecommendationBusinessException(
			RecommendationErrorCode.RECOMMENDATION_RESULT_IO_ERROR,
			detail == null ? RecommendationErrorCode.RECOMMENDATION_RESULT_IO_ERROR.getMessage() : detail);
	}

	public static RecommendationBusinessException resultValidationFailed(String detail) {
		return new RecommendationBusinessException(
			RecommendationErrorCode.RECOMMENDATION_RESULT_VALIDATION_FAILED,
			detail == null ? RecommendationErrorCode.RECOMMENDATION_RESULT_VALIDATION_FAILED.getMessage() : detail);
	}

	public static RecommendationBusinessException resultPollingTimeout(String detail) {
		return new RecommendationBusinessException(
			RecommendationErrorCode.RECOMMENDATION_RESULT_POLLING_TIMEOUT,
			detail == null ? RecommendationErrorCode.RECOMMENDATION_RESULT_POLLING_TIMEOUT.getMessage() : detail);
	}
}
