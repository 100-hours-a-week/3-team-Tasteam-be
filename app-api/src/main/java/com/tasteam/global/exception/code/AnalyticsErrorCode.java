package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnalyticsErrorCode implements ErrorCode {

	ANALYTICS_INGEST_EMPTY_BATCH(HttpStatus.BAD_REQUEST, "수집할 이벤트가 없습니다."),
	ANALYTICS_INGEST_BATCH_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "한 번에 수집 가능한 이벤트 수를 초과했습니다."),
	ANALYTICS_INGEST_EVENT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않은 이벤트가 포함되어 있습니다."),
	ANALYTICS_INGEST_ANONYMOUS_ID_REQUIRED(HttpStatus.BAD_REQUEST, "익명 이벤트 수집에는 anonymousId가 필요합니다."),
	ANALYTICS_INGEST_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "이벤트 수집 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");

	private final HttpStatus httpStatus;
	private final String message;
}
