package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecommendationErrorCode implements ErrorCode {

	RECOMMENDATION_MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "추천 모델 정보를 찾을 수 없습니다."),
	RECOMMENDATION_ACTIVE_MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "활성화된 추천 모델이 없습니다."),
	RECOMMENDATION_PIPELINE_VERSION_MISMATCH(HttpStatus.BAD_REQUEST, "요청한 모델 버전과 결과 파일 버전이 일치하지 않습니다."),
	RECOMMENDATION_CSV_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "추천 결과 파일 형식이 올바르지 않습니다."),
	RECOMMENDATION_RESULT_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "추천 결과 데이터 검증에 실패했습니다.");

	private final HttpStatus httpStatus;
	private final String message;
}
