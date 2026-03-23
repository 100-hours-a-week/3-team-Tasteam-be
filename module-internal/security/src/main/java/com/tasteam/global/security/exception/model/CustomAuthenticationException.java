package com.tasteam.global.security.exception.model;

import org.springframework.security.core.AuthenticationException;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;

/**
 * 커스텀 인증 예외 (401 Unauthorized)
 */
@Getter
public class CustomAuthenticationException extends AuthenticationException {

	private final ErrorCode errorCode;

	public CustomAuthenticationException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public CustomAuthenticationException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

}
