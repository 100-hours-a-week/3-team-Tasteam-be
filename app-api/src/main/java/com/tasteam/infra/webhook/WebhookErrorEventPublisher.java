package com.tasteam.infra.webhook;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.infra.webhook.event.ErrorContext;
import com.tasteam.infra.webhook.event.ErrorOccurredEvent;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.webhook", name = "enabled", havingValue = "true")
public class WebhookErrorEventPublisher {

	private final ApplicationEventPublisher eventPublisher;
	private final WebhookProperties webhookProperties;

	public void publishBusinessException(BusinessException e, HttpServletRequest request) {
		try {
			ErrorContext context = ErrorContext.from(e, request, webhookProperties.isIncludeStackTrace());
			eventPublisher.publishEvent(new ErrorOccurredEvent(context));
		} catch (Exception ex) {
			log.error("비즈니스 예외 이벤트 발행 실패", ex);
		}
	}

	public void publishSystemException(Exception e, HttpServletRequest request) {
		try {
			ErrorContext context = ErrorContext.from(e, request, webhookProperties.isIncludeStackTrace());
			eventPublisher.publishEvent(new ErrorOccurredEvent(context));
		} catch (Exception ex) {
			log.error("시스템 예외 이벤트 발행 실패", ex);
		}
	}

	public void publishValidationException(Exception e, HttpServletRequest request) {
		try {
			ErrorContext context = ErrorContext.fromValidation(e, request, webhookProperties.isIncludeStackTrace());
			eventPublisher.publishEvent(new ErrorOccurredEvent(context));
		} catch (Exception ex) {
			log.error("검증 예외 이벤트 발행 실패", ex);
		}
	}

	public void publishSecurityException(Exception e, HttpServletRequest request, HttpStatus httpStatus,
		String errorCode) {
		try {
			ErrorContext context = ErrorContext.fromSecurity(e, request, httpStatus, errorCode,
				webhookProperties.isIncludeStackTrace());
			eventPublisher.publishEvent(new ErrorOccurredEvent(context));
		} catch (Exception ex) {
			log.error("보안 예외 이벤트 발행 실패", ex);
		}
	}
}
