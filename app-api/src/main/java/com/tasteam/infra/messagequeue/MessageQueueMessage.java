package com.tasteam.infra.messagequeue;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record MessageQueueMessage(
	String topic,
	String key,
	byte[] payload,
	Map<String, String> headers,
	Instant occurredAt,
	String messageId) {

	public MessageQueueMessage {
		if (topic == null || topic.isBlank()) {
			throw new IllegalArgumentException("topic은 필수입니다");
		}
		Objects.requireNonNull(payload, "payload는 필수입니다");

		payload = payload.clone();
		headers = headers == null ? Map.of() : Map.copyOf(headers);
		occurredAt = occurredAt == null ? Instant.now() : occurredAt;
		messageId = (messageId == null || messageId.isBlank()) ? UUID.randomUUID().toString() : messageId;
	}

	public static MessageQueueMessage of(String topic, String key, byte[] payload) {
		return new MessageQueueMessage(topic, key, payload, Map.of(), Instant.now(), null);
	}
}
