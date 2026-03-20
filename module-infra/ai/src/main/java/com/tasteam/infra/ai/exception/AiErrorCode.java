package com.tasteam.infra.ai.exception;

import org.springframework.http.HttpStatus;

import com.tasteam.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiErrorCode implements ErrorCode {

	AI_REQUEST_INVALID(HttpStatus.BAD_REQUEST, "AI 요청이 올바르지 않습니다"),
	AI_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AI 서버 오류가 발생했습니다"),
	AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI 서버 응답이 지연되었습니다"),
	AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버에 연결할 수 없습니다");

	private final HttpStatus httpStatus;
	private final String message;
}
