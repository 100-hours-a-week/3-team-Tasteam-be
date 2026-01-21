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
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 유효한 인증 코드가 존재합니다");

	private final HttpStatus httpStatus;
	private final String message;
}
