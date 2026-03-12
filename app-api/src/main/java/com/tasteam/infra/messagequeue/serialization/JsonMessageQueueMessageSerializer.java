package com.tasteam.infra.messagequeue.serialization;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.infra.messagequeue.MessageQueueMessage;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JsonMessageQueueMessageSerializer implements MessageQueueMessageSerializer {

	private final ObjectMapper objectMapper;

	@Override
	public String serialize(MessageQueueMessage message) {
		try {
			MessageQueueEnvelope envelope = new MessageQueueEnvelope(
				message.topic(),
				message.key(),
				Base64.getEncoder().encodeToString(message.payload()),
				message.headers(),
				message.occurredAt().toEpochMilli(),
				message.messageId());
			return objectMapper.writeValueAsString(envelope);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("메시지 직렬화에 실패했습니다", ex);
		}
	}

	@Override
	public MessageQueueMessage deserialize(String serialized) {
		if (serialized == null || serialized.isBlank()) {
			throw new IllegalArgumentException("역직렬화할 문자열이 비어 있습니다");
		}

		try {
			MessageQueueEnvelope envelope = objectMapper.readValue(serialized, MessageQueueEnvelope.class);
			byte[] payload = Base64.getDecoder().decode(emptyToDefault(envelope.payloadBase64(), ""));

			return new MessageQueueMessage(
				emptyToDefault(envelope.topic(), "unknown"),
				blankToNull(envelope.key()),
				payload,
				envelope.headers() == null ? Map.of() : envelope.headers(),
				Instant.ofEpochMilli(envelope.occurredAtEpochMillis()),
				envelope.messageId());
		} catch (IllegalArgumentException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IllegalArgumentException("메시지 역직렬화에 실패했습니다: "
				+ truncate(serialized), ex);
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
}
