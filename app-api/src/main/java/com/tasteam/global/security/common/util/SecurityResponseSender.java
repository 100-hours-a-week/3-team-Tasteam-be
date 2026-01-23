package com.tasteam.global.security.common.util;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.global.dto.api.ErrorResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.exception.code.AuthErrorCode;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security 필터/핸들러에서 사용하는 공통 응답 전송 유틸리티.
 * <p>
 * - JSON 헤더 설정
 * - 상태 코드 설정
 */
@Component
@RequiredArgsConstructor
public class SecurityResponseSender {

	private final ObjectMapper objectMapper;

	/**
	 * 상태 코드와 메시지를 지정해 에러 JSON을 전송한다.
	 */
	public void sendError(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ErrorResponse<Void> errorResponse = ErrorResponse.of(message);
		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}

	/**
	 * body 포함 성공 응답을 전송한다.
	 */
	public <T> void sendSuccess(HttpServletResponse response, int status, T body) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		SuccessResponse<T> apiResponse = SuccessResponse.success(body);
		response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
	}

	public void sendError(HttpServletResponse response, AuthErrorCode authErrorCode) throws IOException {
		response.setStatus(authErrorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ErrorResponse<Void> errorResponse = ErrorResponse.of(authErrorCode.toString(), authErrorCode.getMessage());
		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}
}
