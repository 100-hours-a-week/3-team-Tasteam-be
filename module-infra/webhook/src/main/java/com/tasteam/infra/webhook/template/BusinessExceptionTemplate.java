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
		fields.put("Endpoint", context.requestMethod() + " " + context.requestPath());
		fields.put("Time", context.timestamp().toString());

		return new WebhookMessage(
			"🚨 " + context.errorCode(),
			context.message(),
			fields,
			"#FFA500",
			context.timestamp(),
			context.stackTrace());
	}
}
