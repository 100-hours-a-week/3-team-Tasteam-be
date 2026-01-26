package com.tasteam.global.security.exception.handler;

import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.security.common.util.SecurityResponseSender;
import com.tasteam.global.security.exception.notifier.SecurityErrorNotifier;

import jakarta.servlet.ServletException;

class CustomAccessDeniedHandlerTest {

	@Test
	void handle_publishesWebhookWhenPublisherIsAvailable() throws IOException, ServletException {
		SecurityResponseSender responseSender = org.mockito.Mockito.mock(SecurityResponseSender.class);
		SecurityErrorNotifier securityErrorNotifier = org.mockito.Mockito.mock(SecurityErrorNotifier.class);

		CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(responseSender, securityErrorNotifier);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AccessDeniedException ex = new AccessDeniedException("denied");

		handler.handle(request, response, ex);

		verify(securityErrorNotifier).notify(AuthErrorCode.ACCESS_DENIED, ex, request);
		verify(responseSender).sendError(response, AuthErrorCode.ACCESS_DENIED);
	}
}
