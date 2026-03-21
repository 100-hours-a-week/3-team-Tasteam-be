package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Redis) RedisStreamMessageQueueProducer 단위 테스트")
class RedisStreamMessageQueueProducerTest {

	@Test
	@DisplayName("메시지를 발행하면 topic-prefix 기반 stream 키로 저장한다")
	void publish_writesRecordToPrefixedStreamKey() throws Exception {
		// given
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked") StreamOperations<String, Object, Object> streamOperations = mock(
			StreamOperations.class);
		when(redisTemplate.opsForStream()).thenReturn(streamOperations);

		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setTopicPrefix("tasteam");
		RedisStreamMessageQueueProducer producer = new RedisStreamMessageQueueProducer(redisTemplate, properties);

		JsonNode payload = new ObjectMapper().readTree("{\"data\":\"payload\"}");
		QueueMessage message = new QueueMessage(
			"order.created",
			"order-1",
			payload,
			Map.of("source", "test"),
			Instant.parse("2026-02-14T00:00:00Z"),
			"msg-1");

		// when
		producer.publish(message);

		// then
		@SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<MapRecord<String, String, String>> recordCaptor = org.mockito.ArgumentCaptor
			.forClass((Class)MapRecord.class);
		verify(streamOperations).add(recordCaptor.capture(), any(XAddOptions.class));

		MapRecord<String, String, String> captured = recordCaptor.getValue();
		assertThat(captured.getStream()).isEqualTo("tasteam:order.created");
		assertThat(captured.getValue().get("messageId")).isEqualTo("msg-1");
		assertThat(captured.getValue().get("topic")).isEqualTo("order.created");
		assertThat(captured.getValue().get("key")).isEqualTo("order-1");
		assertThat(captured.getValue().get("header.source")).isEqualTo("test");
		assertThat(captured.getValue().get("payload")).isEqualTo("{\"data\":\"payload\"}");
	}
}
