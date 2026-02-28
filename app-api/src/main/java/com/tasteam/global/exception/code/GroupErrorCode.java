package com.tasteam.global.exception.code;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GroupErrorCode implements ErrorCode {

	GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹을 찾을 수 없습니다"),
	ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 그룹입니다"),
	EMAIL_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "이메일 인증 토큰이 유효하지 않습니다"),
	EMAIL_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "이메일 인증 토큰이 만료되었습니다"),
	GROUP_PASSWORD_MISMATCH(HttpStatus.CONFLICT, "비밀번호가 일치하지 않습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
