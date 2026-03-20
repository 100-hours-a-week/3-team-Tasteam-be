package com.tasteam.infra.webhook;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.tasteam.infra.webhook.event.ErrorContext;
import com.tasteam.infra.webhook.event.ErrorOccurredEvent;
import com.tasteam.infra.webhook.template.BusinessExceptionTemplate;
import com.tasteam.infra.webhook.template.SystemExceptionTemplate;
import com.tasteam.infra.webhook.template.WebhookMessageTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.webhook", name = "enabled", havingValue = "true")
public class WebhookErrorEventListener {

	private final WebhookClient webhookClient;
	private final BusinessExceptionTemplate businessExceptionTemplate;
	private final SystemExceptionTemplate systemExceptionTemplate;
	private final WebhookProperties webhookProperties;

	@Async("webhookExecutor")
	@EventListener
	public void onErrorOccurred(ErrorOccurredEvent event) {
		try {
			if (!webhookClient.isEnabled()) {
				log.debug("웹훅이 비활성화되어 에러 이벤트를 건너뜁니다: {}", event);
				return;
			}

			ErrorContext context = event.context();

			if (shouldFilterOut(context)) {
				return;
			}

			WebhookMessage message = selectTemplate(context).build(context);

			webhookClient.send(message);
			log.info("에러 웹훅 전송 완료. type={}, code={}, status={}",
				context.errorType(), context.errorCode(), context.httpStatus());

		} catch (Exception e) {
			log.error("에러 이벤트 웹훅 전송 실패: {}", event.context(), e);
		}
	}

	private boolean shouldFilterOut(ErrorContext context) {
		if (webhookProperties.getFilters().getExcludeErrorCodes().contains(context.errorCode())) {
			log.debug("제외된 에러 코드로 인해 웹훅을 건너뜁니다: {}", context.errorCode());
			return true;
		}

		if (context.httpStatus().value() < webhookProperties.getFilters().getMinHttpStatus()) {
			log.debug("낮은 우선순위 에러로 인해 웹훅을 건너뜁니다: {}", context.httpStatus());
			return true;
		}

		return false;
	}

	private WebhookMessageTemplate<ErrorContext> selectTemplate(ErrorContext context) {
		return switch (context.errorType()) {
			case "BUSINESS", "VALIDATION" -> businessExceptionTemplate;
			case "SYSTEM" -> systemExceptionTemplate;
			default -> systemExceptionTemplate;
		};
	}
}
