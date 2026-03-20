package com.tasteam.global.security.exception.handler;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.security.common.util.SecurityResponseSender;
import com.tasteam.global.security.exception.notifier.SecurityErrorNotifier;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 커스텀 인가 접근 거부 핸들러 (403 Forbidden)
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityResponseSender securityResponseSender;
	private final SecurityErrorNotifier securityErrorNotifier;

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
		AccessDeniedException accessDeniedException) throws IOException, ServletException {

		if (isAdminSpaRequest(request)) {
			response.sendRedirect("/admin/");
			return;
		}

		securityErrorNotifier.notify(AuthErrorCode.ACCESS_DENIED, accessDeniedException, request);

		// 403 Forbidden 응답 전송
		securityResponseSender.sendError(response, AuthErrorCode.ACCESS_DENIED);
	}

	private boolean isAdminSpaRequest(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String accept = request.getHeader("Accept");

		return (uri.startsWith("/admin/") || "/admin".equals(uri))
			&& accept != null
			&& accept.contains("text/html")
			&& !uri.startsWith("/admin/js/")
			&& !uri.startsWith("/admin/css/")
			&& !uri.equals("/admin/index.html");
	}
}
