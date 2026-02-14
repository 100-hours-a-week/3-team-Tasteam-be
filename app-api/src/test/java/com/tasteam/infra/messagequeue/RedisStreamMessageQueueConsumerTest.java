package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("Redis Stream 메시지큐 consumer")
class RedisStreamMessageQueueConsumerTest {

	@Test
	@DisplayName("구독하면 컨슈머 그룹을 준비하고 리스너를 등록한다")
	void subscribe_registersListener() {
		// given
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisCallback<Void>>any())).thenReturn(null);

		@SuppressWarnings("unchecked") StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer = mock(
			StreamMessageListenerContainer.class);
		Subscription subscriptionHandle = mock(Subscription.class);
		when(listenerContainer.receive(any(Consumer.class), any(StreamOffset.class), any(StreamListener.class)))
			.thenReturn(subscriptionHandle);

		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setTopicPrefix("tasteam");
		RedisStreamMessageQueueConsumer consumer = new RedisStreamMessageQueueConsumer(
			redisTemplate,
			listenerContainer,
			properties);
		MessageQueueSubscription subscription = new MessageQueueSubscription("order.created", "group-1", "consumer-1");

		// when
		consumer.subscribe(subscription, message -> {});

		// then
		verify(listenerContainer).start();
		verify(listenerContainer).receive(any(Consumer.class), any(StreamOffset.class), any(StreamListener.class));
	}

	@Test
	@DisplayName("구독 해제하면 등록된 리스너를 취소한다")
	void unsubscribe_cancelsSubscription() {
		// given
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisCallback<Void>>any())).thenReturn(null);

		@SuppressWarnings("unchecked") StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer = mock(
			StreamMessageListenerContainer.class);
		Subscription subscriptionHandle = mock(Subscription.class);
		when(listenerContainer.receive(any(Consumer.class), any(StreamOffset.class), any(StreamListener.class)))
			.thenReturn(subscriptionHandle);

		MessageQueueProperties properties = new MessageQueueProperties();
		RedisStreamMessageQueueConsumer consumer = new RedisStreamMessageQueueConsumer(
			redisTemplate,
			listenerContainer,
			properties);
		MessageQueueSubscription subscription = new MessageQueueSubscription("order.created", "group-1", "consumer-1");
		consumer.subscribe(subscription, message -> {});

		// when
		consumer.unsubscribe(subscription);

		// then
		verify(subscriptionHandle).cancel();
	}

	@Test
	@DisplayName("수신한 레코드를 핸들러로 전달하고 ack 처리한다")
	void subscribe_onMessage_callsHandlerAndAck() {
		// given
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked") StreamOperations<String, Object, Object> streamOperations = mock(
			StreamOperations.class);
		when(redisTemplate.opsForStream()).thenReturn(streamOperations);
		when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisCallback<Void>>any())).thenReturn(null);

		@SuppressWarnings("unchecked") StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer = mock(
			StreamMessageListenerContainer.class);
		Subscription subscriptionHandle = mock(Subscription.class);
		when(listenerContainer.receive(any(Consumer.class), any(StreamOffset.class), any(StreamListener.class)))
			.thenReturn(subscriptionHandle);

		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setTopicPrefix("tasteam");
		RedisStreamMessageQueueConsumer consumer = new RedisStreamMessageQueueConsumer(
			redisTemplate,
			listenerContainer,
			properties);
		MessageQueueSubscription subscription = new MessageQueueSubscription("order.created", "group-1", "consumer-1");

		java.util.concurrent.atomic.AtomicReference<MessageQueueMessage> captured = new java.util.concurrent.atomic.AtomicReference<>();
		consumer.subscribe(subscription, captured::set);

		@SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<StreamListener<String, MapRecord<String, String, String>>> listenerCaptor = org.mockito.ArgumentCaptor
			.forClass((Class)StreamListener.class);
		verify(listenerContainer).receive(any(Consumer.class), any(StreamOffset.class), listenerCaptor.capture());

		MapRecord<String, String, String> record = mock(MapRecord.class);
		when(record.getStream()).thenReturn("tasteam:order.created");
		when(record.getId()).thenReturn(RecordId.of("1-0"));
		when(record.getValue()).thenReturn(Map.of(
			"messageId", "msg-1",
			"topic", "order.created",
			"key", "order-1",
			"occurredAt", String.valueOf(Instant.parse("2026-02-14T00:00:00Z").toEpochMilli()),
			"payload", Base64.getEncoder().encodeToString("payload".getBytes(StandardCharsets.UTF_8)),
			"header.source", "test"));

		// when
		listenerCaptor.getValue().onMessage(record);

		// then
		assertThat(captured.get()).isNotNull();
		assertThat(captured.get().topic()).isEqualTo("order.created");
		assertThat(captured.get().key()).isEqualTo("order-1");
		assertThat(new String(captured.get().payload(), StandardCharsets.UTF_8)).isEqualTo("payload");
		assertThat(captured.get().headers()).containsEntry("source", "test");
		verify(streamOperations).acknowledge(eq("tasteam:order.created"), eq("group-1"), eq(RecordId.of("1-0")));
	}

	@Test
	@DisplayName("occurredAt 값이 숫자가 아니어도 예외 없이 현재 시간 기준으로 처리한다")
	void subscribe_onMessage_withInvalidOccurredAt_handlesGracefully() {
		// given
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked") StreamOperations<String, Object, Object> streamOperations = mock(
			StreamOperations.class);
		when(redisTemplate.opsForStream()).thenReturn(streamOperations);
		when(redisTemplate.execute(org.mockito.ArgumentMatchers.<RedisCallback<Void>>any())).thenReturn(null);

		@SuppressWarnings("unchecked") StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer = mock(
			StreamMessageListenerContainer.class);
		Subscription subscriptionHandle = mock(Subscription.class);
		when(listenerContainer.receive(any(Consumer.class), any(StreamOffset.class), any(StreamListener.class)))
			.thenReturn(subscriptionHandle);

		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setTopicPrefix("tasteam");
		RedisStreamMessageQueueConsumer consumer = new RedisStreamMessageQueueConsumer(
			redisTemplate,
			listenerContainer,
			properties);
		MessageQueueSubscription subscription = new MessageQueueSubscription("order.created", "group-1", "consumer-1");

		java.util.concurrent.atomic.AtomicReference<MessageQueueMessage> captured = new java.util.concurrent.atomic.AtomicReference<>();
		consumer.subscribe(subscription, captured::set);

		@SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<StreamListener<String, MapRecord<String, String, String>>> listenerCaptor = org.mockito.ArgumentCaptor
			.forClass((Class)StreamListener.class);
		verify(listenerContainer).receive(any(Consumer.class), any(StreamOffset.class), listenerCaptor.capture());

		MapRecord<String, String, String> record = mock(MapRecord.class);
		when(record.getStream()).thenReturn("tasteam:order.created");
		when(record.getId()).thenReturn(RecordId.of("1-1"));
		when(record.getValue()).thenReturn(Map.of(
			"messageId", "msg-2",
			"topic", "order.created",
			"key", "order-2",
			"occurredAt", "invalid-number",
			"payload", Base64.getEncoder().encodeToString("payload".getBytes(StandardCharsets.UTF_8))));

		// when
		listenerCaptor.getValue().onMessage(record);

		// then
		assertThat(captured.get()).isNotNull();
		assertThat(captured.get().occurredAt()).isNotNull();
		verify(streamOperations).acknowledge(eq("tasteam:order.created"), eq("group-1"), eq(RecordId.of("1-1")));
	}
}
