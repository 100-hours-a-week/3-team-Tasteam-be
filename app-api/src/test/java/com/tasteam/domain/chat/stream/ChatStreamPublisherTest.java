package com.tasteam.domain.chat.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.chat.config.ChatStreamProperties;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.type.ChatMessageType;

@UnitTest
@DisplayName("[유닛](Chat) ChatStreamPublisher 단위 테스트")
class ChatStreamPublisherTest {

	private final ChatStreamKeyResolver keyResolver = new ChatStreamKeyResolver();

	@Test
	@DisplayName("파티션 소비가 활성화되면 파티션 stream에만 발행한다")
	void publish_partitionOnly() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked") StreamOperations<String, Object, Object> streamOperations = mock(
			StreamOperations.class);
		when(redisTemplate.opsForStream()).thenReturn(streamOperations);

		ChatStreamProperties properties = new ChatStreamProperties(
			true,
			true,
			100,
			1000,
			4,
			256,
			16,
			128,
			true,
			false,
			false);

		ChatStreamPublisher publisher = new ChatStreamPublisher(redisTemplate, keyResolver, properties);
		publisher.publish(33L, sampleMessage());

		@SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<MapRecord<String, String, String>> recordCaptor = org.mockito.ArgumentCaptor
			.forClass(
				(Class)MapRecord.class);
		verify(streamOperations).add(recordCaptor.capture(), any(XAddOptions.class));

		assertThat(recordCaptor.getValue().getStream()).isEqualTo("chat:partition:1");
	}

	@Test
	@DisplayName("dual-write가 활성화되면 파티션 stream과 room stream에 모두 발행한다")
	void publish_dualWrite() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked") StreamOperations<String, Object, Object> streamOperations = mock(
			StreamOperations.class);
		when(redisTemplate.opsForStream()).thenReturn(streamOperations);

		ChatStreamProperties properties = new ChatStreamProperties(
			true,
			true,
			100,
			1000,
			4,
			256,
			16,
			128,
			true,
			true,
			false);

		ChatStreamPublisher publisher = new ChatStreamPublisher(redisTemplate, keyResolver, properties);
		publisher.publish(33L, sampleMessage());

		@SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<MapRecord<String, String, String>> recordCaptor = org.mockito.ArgumentCaptor
			.forClass(
				(Class)MapRecord.class);
		verify(streamOperations, org.mockito.Mockito.times(2)).add(recordCaptor.capture(), any(XAddOptions.class));

		List<String> streamKeys = recordCaptor.getAllValues().stream().map(MapRecord::getStream).toList();
		assertThat(streamKeys).containsExactlyInAnyOrder("chat:partition:1", "chat:room:33");
	}

	@Test
	@DisplayName("파티션 소비가 비활성화되면 레거시 room stream에만 발행한다")
	void publish_legacyOnly() {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		@SuppressWarnings("unchecked") StreamOperations<String, Object, Object> streamOperations = mock(
			StreamOperations.class);
		when(redisTemplate.opsForStream()).thenReturn(streamOperations);

		ChatStreamProperties properties = new ChatStreamProperties(
			true,
			true,
			100,
			1000,
			4,
			256,
			16,
			128,
			false,
			true,
			true);

		ChatStreamPublisher publisher = new ChatStreamPublisher(redisTemplate, keyResolver, properties);
		publisher.publish(33L, sampleMessage());

		@SuppressWarnings("unchecked") org.mockito.ArgumentCaptor<MapRecord<String, String, String>> recordCaptor = org.mockito.ArgumentCaptor
			.forClass(
				(Class)MapRecord.class);
		verify(streamOperations).add(recordCaptor.capture(), any(XAddOptions.class));

		assertThat(recordCaptor.getValue().getStream()).isEqualTo("chat:room:33");
	}

	private ChatMessageItemResponse sampleMessage() {
		return new ChatMessageItemResponse(
			101L,
			7L,
			"member",
			null,
			"hello",
			ChatMessageType.TEXT,
			List.of(),
			Instant.parse("2026-03-11T00:00:00Z"));
	}
}
