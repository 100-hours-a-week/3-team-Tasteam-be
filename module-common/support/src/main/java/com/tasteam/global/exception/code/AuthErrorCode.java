package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 존재하지 않습니다."),
	REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),
	REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
	REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 재사용되었습니다."),
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 이메일입니다."),
	NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 닉네임입니다."),
	PASSWORD_TOO_WEAK(HttpStatus.BAD_REQUEST, "비밀번호는 8자 이상이어야 합니다."),
	INVALID_ADMIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "관리자 계정 정보가 올바르지 않습니다.")

	;

	private final HttpStatus httpStatus;
	private final String message;
}
