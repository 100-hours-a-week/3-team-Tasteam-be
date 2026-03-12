package com.tasteam.infra.messagequeue.serialization;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public record QueueMessageEnvelope(
	String topic,
	String key,
	JsonNode payload,
	Map<String, String> headers,
	long occurredAtEpochMillis,
	String messageId) {
}
