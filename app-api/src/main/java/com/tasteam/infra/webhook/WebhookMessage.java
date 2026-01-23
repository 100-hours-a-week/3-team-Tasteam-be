package com.tasteam.infra.webhook;

import java.time.Instant;
import java.util.Map;

public record WebhookMessage(
	String title,
	String description,
	Map<String, String> fields,
	String color,
	Instant timestamp,
	String stackTrace) {
}
