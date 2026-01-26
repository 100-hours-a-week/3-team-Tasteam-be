package com.tasteam.global.security.exception.handler;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.security.common.util.SecurityResponseSender;
import com.tasteam.global.security.exception.notifier.SecurityErrorNotifier;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 커스텀 인증 예외 (401 Unauthorized) 핸들러
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityResponseSender securityResponseSender;
	private final SecurityErrorNotifier securityErrorNotifier;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException authException) throws IOException, ServletException {

		securityErrorNotifier.notify(AuthErrorCode.AUTHENTICATION_REQUIRED, authException, request);

		// 401 Unauthorized 응답 전송
		securityResponseSender.sendError(response, AuthErrorCode.AUTHENTICATION_REQUIRED);
	}
}
