package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {

	AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH-001", "인증이 필요합니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-002", "접근 권한이 없습니다."),
	REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH-003", "리프레시 토큰이 존재하지 않습니다."),
	REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH-004", "리프레시 토큰이 유효하지 않습니다."),
	REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH-005", "리프레시 토큰이 만료되었습니다."),
	REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "AUTH-009", "리프레시 토큰이 재사용되었습니다."),
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH-006", "이미 등록된 이메일입니다."),
	NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH-007", "이미 등록된 닉네임입니다."),
	PASSWORD_TOO_WEAK(HttpStatus.BAD_REQUEST, "AUTH-008", "비밀번호는 8자 이상이어야 합니다.");

	;

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	AuthErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
