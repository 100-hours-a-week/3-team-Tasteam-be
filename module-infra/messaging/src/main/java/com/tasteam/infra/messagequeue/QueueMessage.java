package com.tasteam.infra.messagequeue;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

public record QueueMessage(
	String topic,
	String key,
	JsonNode payload,
	Map<String, String> headers,
	Instant occurredAt,
	String messageId) {

	public QueueMessage {
		if (topic == null || topic.isBlank()) {
			throw new IllegalArgumentException("topic은 필수입니다");
		}
		Objects.requireNonNull(payload, "payload는 필수입니다");

		headers = headers == null ? Map.of() : Map.copyOf(headers);
		occurredAt = occurredAt == null ? Instant.now() : occurredAt;
		messageId = (messageId == null || messageId.isBlank()) ? UUID.randomUUID().toString() : messageId;
	}

	public static QueueMessage of(String topic, String key, JsonNode payload) {
		return new QueueMessage(topic, key, payload, Map.of(), Instant.now(), null);
	}
}
