package com.tasteam.global.security.exception.model;

import org.springframework.security.access.AccessDeniedException;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;

/**
 * 커스텀 인가 접근 거부 예외 (403 Forbidden)
 */
@Getter
public class CustomAccessDeniedException extends AccessDeniedException {

	private final ErrorCode errorCode;

	public CustomAccessDeniedException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public CustomAccessDeniedException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}
