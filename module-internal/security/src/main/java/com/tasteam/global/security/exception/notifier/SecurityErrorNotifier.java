package com.tasteam.global.security.exception.notifier;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.infra.webhook.WebhookErrorEventPublisher;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityErrorNotifier {

	private final ObjectProvider<WebhookErrorEventPublisher> webhookErrorEventPublisherProvider;

	public void notify(AuthErrorCode errorCode, Exception exception, HttpServletRequest request) {
		WebhookErrorEventPublisher webhookErrorEventPublisher = webhookErrorEventPublisherProvider.getIfAvailable();
		if (webhookErrorEventPublisher == null) {
			return;
		}

		webhookErrorEventPublisher.publishSecurityException(
			exception,
			request,
			errorCode.getHttpStatus(),
			errorCode.toString());
	}
}
