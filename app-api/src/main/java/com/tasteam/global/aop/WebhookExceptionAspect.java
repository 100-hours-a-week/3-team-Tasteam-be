package com.tasteam.global.aop;

import java.util.Optional;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.infra.webhook.WebhookErrorEventPublisher;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.webhook", name = "enabled", havingValue = "true")
public class WebhookExceptionAspect {

	private final WebhookErrorEventPublisher webhookPublisher;

	@AfterReturning("execution(* com.tasteam.global.exception.handler.GlobalExceptionHandler.*(..))")
	public void sendWebhookOnException(JoinPoint joinPoint) {
		try {
			HttpServletRequest request = getCurrentRequest();
			Object[] args = joinPoint.getArgs();

			if (args.length > 0 && args[0] instanceof Throwable ex) {
				if (ex instanceof BusinessException businessException) {
					webhookPublisher.publishBusinessException(businessException, request);
				} else if (ex instanceof BindException bindException) {
					webhookPublisher.publishValidationException(bindException, request);
				} else if (ex instanceof Exception exception) {
					webhookPublisher.publishSystemException(exception, request);
				}
			}
		} catch (Exception e) {
			log.error("웹훅 AOP에서 예외 발생", e);
		}
	}

	private HttpServletRequest getCurrentRequest() {
		ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
		return Optional.ofNullable(attributes)
			.map(ServletRequestAttributes::getRequest)
			.orElse(null);
	}
}
