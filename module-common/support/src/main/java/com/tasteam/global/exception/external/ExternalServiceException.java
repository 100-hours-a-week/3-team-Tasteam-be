package com.tasteam.global.exception.external;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;

@Getter
public class ExternalServiceException extends RuntimeException {

	private final ErrorCode errorCode;
	private final String externalRequestId;

	public ExternalServiceException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.externalRequestId = null;
	}

	public ExternalServiceException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
		this.externalRequestId = null;
	}

	public ExternalServiceException(ErrorCode errorCode, String message, String externalRequestId) {
		super(message);
		this.errorCode = errorCode;
		this.externalRequestId = externalRequestId;
	}

	public String getErrorCode() {
		return errorCode.name();
	}

	public String getErrorMessage() {
		return errorCode.getMessage();
	}

	public HttpStatus getHttpStatus() {
		return this.errorCode.getHttpStatus();
	}
}
