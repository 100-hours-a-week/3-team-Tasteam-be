package com.tasteam.infra.webhook.template;

import com.tasteam.infra.webhook.WebhookMessage;

public interface WebhookMessageTemplate<T> {

	WebhookMessage build(T data);
}
