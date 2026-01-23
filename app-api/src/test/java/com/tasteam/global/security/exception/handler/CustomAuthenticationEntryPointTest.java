package com.tasteam.global.security.exception.handler;

import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.security.common.util.SecurityResponseSender;
import com.tasteam.global.security.exception.notifier.SecurityErrorNotifier;

import jakarta.servlet.ServletException;

class CustomAuthenticationEntryPointTest {

	@Test
	void commence_publishesWebhookWhenPublisherIsAvailable() throws IOException, ServletException {
		SecurityResponseSender responseSender = org.mockito.Mockito.mock(SecurityResponseSender.class);
		SecurityErrorNotifier securityErrorNotifier = org.mockito.Mockito.mock(SecurityErrorNotifier.class);
		CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint(responseSender,
			securityErrorNotifier);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		BadCredentialsException ex = new BadCredentialsException("bad credentials");

		entryPoint.commence(request, response, ex);

		verify(securityErrorNotifier).notify(AuthErrorCode.AUTHENTICATION_REQUIRED, ex, request);
		verify(responseSender).sendError(response, AuthErrorCode.AUTHENTICATION_REQUIRED);
	}
}
