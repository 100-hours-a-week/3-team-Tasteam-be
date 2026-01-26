package com.tasteam.infra.webhook;

public interface WebhookClient {

	void send(WebhookMessage message);

	boolean isEnabled();
}
