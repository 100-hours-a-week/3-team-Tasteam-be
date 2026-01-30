package com.tasteam.infra.ai.exception;

import com.tasteam.global.exception.external.ExternalServiceException;

public class AiServerException extends ExternalServiceException {

	public AiServerException(AiErrorCode errorCode) {
		super(errorCode);
	}

	public AiServerException(AiErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
