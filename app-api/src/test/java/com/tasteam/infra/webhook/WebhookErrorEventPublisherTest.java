package com.tasteam.infra.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import com.tasteam.infra.webhook.event.ErrorContext;
import com.tasteam.infra.webhook.event.ErrorOccurredEvent;

class WebhookErrorEventPublisherTest {

	@Test
	void publishSecurityException_publishesSecurityContext() {
		ApplicationEventPublisher eventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
		WebhookProperties webhookProperties = new WebhookProperties();
		webhookProperties.setIncludeStackTrace(false);
		WebhookErrorEventPublisher publisher = new WebhookErrorEventPublisher(eventPublisher, webhookProperties);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI("/api/secure");
		request.addHeader("User-Agent", "JUnit");

		RuntimeException ex = new RuntimeException("unauthorized");

		publisher.publishSecurityException(ex, request, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED");

		ArgumentCaptor<ErrorOccurredEvent> captor = ArgumentCaptor.forClass(ErrorOccurredEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());

		ErrorContext context = captor.getValue().context();
		assertThat(context.errorType()).isEqualTo("SECURITY");
		assertThat(context.errorCode()).isEqualTo("AUTHENTICATION_REQUIRED");
		assertThat(context.httpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(context.requestMethod()).isEqualTo("GET");
		assertThat(context.requestPath()).isEqualTo("/api/secure");
		assertThat(context.userAgent()).isEqualTo("JUnit");
	}
}
