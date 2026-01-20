package com.tasteam.global.exception.business;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public HttpStatus getHttpStatus() {
		return this.errorCode.getHttpStatus();
	}

}
