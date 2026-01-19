package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	// 공통 에러
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
	AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),

	// 도메인 검증 에러 (500 - 서버 내부 로직 오류)
	INVALID_DOMAIN_STATE(HttpStatus.INTERNAL_SERVER_ERROR, "도메인 상태가 유효하지 않습니다"),

	// 권한 에러 (403 - Forbidden)
	NO_PERMISSION(HttpStatus.FORBIDDEN, "해당 리소스에 대한 권한이 없습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
