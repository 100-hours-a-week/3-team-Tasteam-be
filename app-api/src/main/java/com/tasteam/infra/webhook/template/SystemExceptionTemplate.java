package com.tasteam.infra.webhook.template;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.tasteam.infra.webhook.WebhookMessage;
import com.tasteam.infra.webhook.event.ErrorContext;

@Component
public class SystemExceptionTemplate implements WebhookMessageTemplate<ErrorContext> {

	@Override
	public WebhookMessage build(ErrorContext context) {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("HTTP Status", "500 Internal Server Error");
		fields.put("Request", context.requestMethod() + " " + context.requestPath());
		fields.put("Exception", context.exceptionClass());
		fields.put("Timestamp", context.timestamp().toString());

		return new WebhookMessage(
			"🚨 시스템 예외 발생",
			context.message(),
			fields,
			"#FF0000",
			context.timestamp());
	}
}
