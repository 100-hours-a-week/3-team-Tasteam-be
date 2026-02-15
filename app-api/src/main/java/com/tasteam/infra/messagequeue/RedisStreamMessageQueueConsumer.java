package com.tasteam.infra.messagequeue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RedisStreamMessageQueueConsumer implements MessageQueueConsumer {

	private final StringRedisTemplate stringRedisTemplate;
	private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamListenerContainer;
	private final MessageQueueProperties properties;
	private final Map<MessageQueueSubscription, Subscription> subscriptions = new ConcurrentHashMap<>();

	@Override
	public void subscribe(MessageQueueSubscription subscription, MessageQueueMessageHandler handler) {
		subscriptions.computeIfAbsent(subscription, key -> registerSubscription(key, handler));
	}

	@Override
	public void unsubscribe(MessageQueueSubscription subscription) {
		Subscription activeSubscription = subscriptions.remove(subscription);
		if (activeSubscription != null) {
			activeSubscription.cancel();
		}
	}

	private Subscription registerSubscription(MessageQueueSubscription subscription,
		MessageQueueMessageHandler handler) {
		String streamKey = streamKey(subscription.topic());
		ensureGroupExists(streamKey, subscription.consumerGroup());
		streamListenerContainer.start();

		Consumer consumer = Consumer.from(subscription.consumerGroup(), subscription.consumerName());
		return streamListenerContainer.receive(
			consumer,
			StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
			record -> handleRecord(record, subscription, handler));
	}

	private void handleRecord(
		MapRecord<String, String, String> record,
		MessageQueueSubscription subscription,
		MessageQueueMessageHandler handler) {
		MessageQueueMessage message = toMessage(record.getValue());
		long startedAtNanos = System.nanoTime();
		try {
			log.info("메시지큐 수신 처리 시작. stream={}, topic={}, messageId={}, consumerGroup={}, consumerName={}",
				record.getStream(), message.topic(), message.messageId(), subscription.consumerGroup(),
				subscription.consumerName());
			handler.handle(message);
			stringRedisTemplate.opsForStream().acknowledge(record.getStream(), subscription.consumerGroup(),
				record.getId());
			log.info("메시지큐 수신 처리 성공. stream={}, topic={}, messageId={}, consumerGroup={}, processingMillis={}",
				record.getStream(), message.topic(), message.messageId(), subscription.consumerGroup(),
				toMillis(startedAtNanos));
		} catch (Exception ex) {
			log.warn("메시지큐 수신 처리 실패. stream={}, topic={}, messageId={}, consumerGroup={}, processingMillis={}",
				record.getStream(), message.topic(), message.messageId(), subscription.consumerGroup(),
				toMillis(startedAtNanos), ex);
		}
	}

	private MessageQueueMessage toMessage(Map<String, String> recordValue) {
		String topic = recordValue.getOrDefault("topic", "unknown");
		String key = emptyToNull(recordValue.get("key"));
		String messageId = recordValue.get("messageId");
		Instant occurredAt = parseOccurredAt(recordValue.get("occurredAt"));
		byte[] payload = java.util.Base64.getDecoder().decode(recordValue.getOrDefault("payload", ""));

		Map<String, String> headers = new HashMap<>();
		recordValue.forEach((field, value) -> {
			if (field.startsWith("header.")) {
				headers.put(field.substring("header.".length()), value);
			}
		});

		return new MessageQueueMessage(topic, key, payload, headers, occurredAt, messageId);
	}

	private Instant parseOccurredAt(String value) {
		if (value == null || value.isBlank()) {
			return Instant.now();
		}
		try {
			return Instant.ofEpochMilli(Long.parseLong(value));
		} catch (NumberFormatException ex) {
			log.warn("메시지큐 레코드의 occurredAt 값이 올바르지 않습니다. value={}", value);
			return Instant.now();
		}
	}

	private String streamKey(String topic) {
		return properties.getTopicPrefix() + ":" + topic;
	}

	private String emptyToNull(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private void ensureGroupExists(String streamKey, String consumerGroup) {
		stringRedisTemplate.execute((RedisCallback<Void>)connection -> {
			try {
				connection.streamCommands().xGroupCreate(
					stringRedisTemplate.getStringSerializer().serialize(streamKey),
					consumerGroup,
					ReadOffset.lastConsumed(),
					true);
			} catch (Exception ex) {
				String message = ex.getMessage();
				if (message == null || !message.contains("BUSYGROUP")) {
					throw ex;
				}
			}
			return null;
		});
	}

	private long toMillis(long startedAtNanos) {
		return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
	}
}
