package com.tasteam.global.security.oauth.handler;

import java.io.IOException;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.global.dto.api.ErrorResponse;
import com.tasteam.global.security.oauth.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.tasteam.infra.webhook.WebhookErrorEventPublisher;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth2 로그인 실패 핸들러: 인증 요청/redirect 쿠키 정리 후 오류 페이지로 리다이렉트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
	private final ObjectMapper objectMapper;
	private final ObjectProvider<WebhookErrorEventPublisher> webhookErrorEventPublisherProvider;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
		AuthenticationException exception) throws IOException, ServletException {
		authorizationRequestRepository.removeAuthorizationRequestCookies(response);
		log.warn("OAuth2 login failed: {}", exception.getMessage());
		sendWebhookIfAvailable(request, exception);
		sendErrorResponse(response, exception);
	}

	private void sendWebhookIfAvailable(HttpServletRequest request, AuthenticationException exception) {
		WebhookErrorEventPublisher webhookErrorEventPublisher = webhookErrorEventPublisherProvider.getIfAvailable();
		if (webhookErrorEventPublisher == null) {
			return;
		}
		webhookErrorEventPublisher.publishSecurityException(
			exception,
			request,
			HttpStatus.UNAUTHORIZED,
			"OAUTH_LOGIN_FAILED");
	}

	private void sendErrorResponse(HttpServletResponse response, AuthenticationException exception) throws IOException {
		response.setStatus(HttpStatus.UNAUTHORIZED.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		ErrorResponse<Void> errorResponse = ErrorResponse.of("OAUTH_LOGIN_FAILED", "인증에 실패했습니다.");
		response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
	}
}
