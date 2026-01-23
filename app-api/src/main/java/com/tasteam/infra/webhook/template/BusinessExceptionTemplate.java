package com.tasteam.infra.webhook.template;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.tasteam.infra.webhook.WebhookMessage;
import com.tasteam.infra.webhook.event.ErrorContext;

@Component
public class BusinessExceptionTemplate implements WebhookMessageTemplate<ErrorContext> {

	@Override
	public WebhookMessage build(ErrorContext context) {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("Error Code", context.errorCode());
		fields.put("HTTP Status", context.httpStatus().toString());
		fields.put("Request", context.requestMethod() + " " + context.requestPath());
		fields.put("Exception", context.exceptionClass());
		fields.put("Timestamp", context.timestamp().toString());

		return new WebhookMessage(
			"⚠️ 비즈니스 예외 발생",
			context.message(),
			fields,
			"#FFA500",
			context.timestamp());
	}
}
