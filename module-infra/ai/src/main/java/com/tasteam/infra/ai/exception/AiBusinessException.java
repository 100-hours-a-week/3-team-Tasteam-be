package com.tasteam.infra.ai.exception;

import com.tasteam.global.exception.external.ExternalServiceException;

public class AiBusinessException extends ExternalServiceException {

	public AiBusinessException(AiErrorCode errorCode) {
		super(errorCode);
	}

	public AiBusinessException(AiErrorCode errorCode, String message) {
		super(errorCode, message);
	}
}
