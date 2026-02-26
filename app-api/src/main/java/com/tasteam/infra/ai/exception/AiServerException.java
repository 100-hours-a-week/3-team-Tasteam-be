package com.tasteam.infra.ai.exception;

import org.springframework.web.client.RestClientResponseException;

import com.tasteam.global.exception.external.ExternalServiceException;

public class AiServerException extends ExternalServiceException {

	public AiServerException(AiErrorCode errorCode) {
		super(errorCode);
	}

	public AiServerException(AiErrorCode errorCode, String message) {
		super(errorCode, message);
	}

	public static AiServerException from(RestClientResponseException e, String requestId) {
		AiErrorCode errorCode = e.getStatusCode().is4xxClientError()
			? AiErrorCode.AI_REQUEST_INVALID
			: AiErrorCode.AI_SERVER_ERROR;
		return new AiServerException(errorCode, e.getResponseBodyAsString());
	}

	public static AiServerException timeout(String requestId) {
		return new AiServerException(AiErrorCode.AI_TIMEOUT, "AI server timeout");
	}

	public static AiServerException unavailable(String requestId, String message) {
		return new AiServerException(AiErrorCode.AI_UNAVAILABLE, message != null ? message : "AI server unavailable");
	}

	public static AiServerException unknown(Exception e, String requestId) {
		return new AiServerException(AiErrorCode.AI_UNAVAILABLE, e.getMessage());
	}
}
