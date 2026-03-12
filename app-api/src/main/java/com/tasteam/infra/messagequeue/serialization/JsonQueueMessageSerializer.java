package com.tasteam.infra.messagequeue.serialization;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.infra.messagequeue.QueueMessage;
import com.tasteam.infra.messagequeue.exception.MessageQueueNonRetryableException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JsonQueueMessageSerializer implements QueueMessageSerializer {

	private final ObjectMapper objectMapper;

	@Override
	public QueueMessage createMessage(String topic, String key, Object payload, Map<String, String> headers) {
		try {
			JsonNode payloadNode = toPayloadNode(payload);
			return new QueueMessage(topic, key, payloadNode, headers, Instant.now(), null);
		} catch (Exception ex) {
			throw new MessageQueueNonRetryableException(
				"메시지 payload 직렬화에 실패했습니다. topic=%s".formatted(topic),
				topic,
				null,
				ex);
		}
	}

	@Override
	public String serialize(QueueMessage message) {
		try {
			QueueMessageEnvelope envelope = new QueueMessageEnvelope(
				message.topic(),
				message.key(),
				message.payload(),
				message.headers(),
				message.occurredAt().toEpochMilli(),
				message.messageId());
			return objectMapper.writeValueAsString(envelope);
		} catch (Exception ex) {
			throw new IllegalArgumentException("메시지 직렬화에 실패했습니다", ex);
		}
	}

	@Override
	public QueueMessage deserialize(String serialized) {
		if (serialized == null || serialized.isBlank()) {
			throw new MessageQueueNonRetryableException("역직렬화할 문자열이 비어 있습니다", "unknown", null);
		}

		try {
			QueueMessageEnvelope envelope = objectMapper.readValue(serialized, QueueMessageEnvelope.class);

			return new QueueMessage(
				emptyToDefault(envelope.topic(), "unknown"),
				blankToNull(envelope.key()),
				envelope.payload() != null ? envelope.payload() : objectMapper.nullNode(),
				envelope.headers() == null ? Map.of() : envelope.headers(),
				Instant.ofEpochMilli(envelope.occurredAtEpochMillis()),
				envelope.messageId());
		} catch (MessageQueueNonRetryableException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new MessageQueueNonRetryableException(
				"메시지 역직렬화에 실패했습니다: " + truncate(serialized),
				"unknown",
				null,
				ex);
		}
	}

	private String emptyToDefault(String value, String defaultValue) {
		return (value == null || value.isBlank()) ? defaultValue : value;
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private String truncate(String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		if (bytes.length <= 120) {
			return value;
		}
		return value.substring(0, Math.min(value.length(), 120)) + "...";
	}

	private JsonNode toPayloadNode(Object payload) {
		if (payload instanceof JsonNode jsonNode) {
			return jsonNode;
		}
		return objectMapper.valueToTree(payload);
	}
}
