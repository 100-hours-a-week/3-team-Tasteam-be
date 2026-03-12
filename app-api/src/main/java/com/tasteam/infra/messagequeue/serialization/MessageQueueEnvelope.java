package com.tasteam.infra.messagequeue.serialization;

import java.util.Map;

public record MessageQueueEnvelope(
	String topic,
	String key,
	String payloadBase64,
	Map<String, String> headers,
	long occurredAtEpochMillis,
	String messageId) {
}
