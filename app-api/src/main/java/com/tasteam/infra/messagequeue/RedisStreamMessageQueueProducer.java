package com.tasteam.infra.messagequeue;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RedisStreamMessageQueueProducer implements MessageQueueProducer {

	private static final int MAX_STREAM_LENGTH = 1000;

	private final StringRedisTemplate stringRedisTemplate;
	private final MessageQueueProperties properties;

	@Override
	public void publish(MessageQueueMessage message) {
		String streamKey = streamKey(message.topic());
		MapRecord<String, String, String> record = StreamRecords.newRecord()
			.in(streamKey)
			.ofMap(toRecordValue(message));

		stringRedisTemplate.opsForStream().add(
			record,
			XAddOptions.maxlen(MAX_STREAM_LENGTH).approximateTrimming(true));
	}

	private String streamKey(String topic) {
		return properties.getTopicPrefix() + ":" + topic;
	}

	private Map<String, String> toRecordValue(MessageQueueMessage message) {
		Map<String, String> value = new HashMap<>();
		value.put("messageId", message.messageId());
		value.put("topic", message.topic());
		value.put("key", message.key() == null ? "" : message.key());
		value.put("occurredAt", String.valueOf(message.occurredAt().toEpochMilli()));
		value.put("payload", java.util.Base64.getEncoder().encodeToString(message.payload()));

		message.headers().forEach((headerKey, headerValue) -> value.put("header." + headerKey, headerValue));
		return value;
	}
}
