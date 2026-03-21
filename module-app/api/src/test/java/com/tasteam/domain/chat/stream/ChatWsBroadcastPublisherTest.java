package com.tasteam.domain.chat.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.chat.config.ChatStreamProperties;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.type.ChatMessageType;

@UnitTest
@DisplayName("[유닛](Chat) ChatWsBroadcastPublisher 단위 테스트")
class ChatWsBroadcastPublisherTest {

	@Test
	@DisplayName("Pub/Sub 발행 성공 시 true를 반환한다")
	void publish_success() {
		StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		ChatStreamGroupNameProvider groupNameProvider = Mockito.mock(ChatStreamGroupNameProvider.class);
		when(groupNameProvider.consumerName()).thenReturn("instance-a");

		ChatWsBroadcastPublisher publisher = new ChatWsBroadcastPublisher(
			redisTemplate,
			objectMapper,
			sampleProperties(),
			groupNameProvider);

		boolean result = publisher.publish(ChatStreamPayload.from(3L, sampleMessage()));

		assertThat(result).isTrue();
		verify(redisTemplate).convertAndSend(eq("chat:websocket:broadcast"), anyString());
	}

	@Test
	@DisplayName("직렬화 실패 시 false를 반환한다")
	void publish_serializeFail() throws Exception {
		StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
		when(objectMapper.writeValueAsString(org.mockito.ArgumentMatchers.any()))
			.thenThrow(new JsonProcessingException("serialize failed") {});
		ChatStreamGroupNameProvider groupNameProvider = Mockito.mock(ChatStreamGroupNameProvider.class);
		when(groupNameProvider.consumerName()).thenReturn("instance-a");

		ChatWsBroadcastPublisher publisher = new ChatWsBroadcastPublisher(
			redisTemplate,
			objectMapper,
			sampleProperties(),
			groupNameProvider);

		boolean result = publisher.publish(ChatStreamPayload.from(3L, sampleMessage()));

		assertThat(result).isFalse();
		verifyNoInteractions(redisTemplate);
	}

	@Test
	@DisplayName("Redis 발행 실패 시 false를 반환한다")
	void publish_redisFail() {
		StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		ChatStreamGroupNameProvider groupNameProvider = Mockito.mock(ChatStreamGroupNameProvider.class);
		when(groupNameProvider.consumerName()).thenReturn("instance-a");
		doThrow(new RuntimeException("redis down"))
			.when(redisTemplate)
			.convertAndSend(eq("chat:websocket:broadcast"), anyString());

		ChatWsBroadcastPublisher publisher = new ChatWsBroadcastPublisher(
			redisTemplate,
			objectMapper,
			sampleProperties(),
			groupNameProvider);

		boolean result = publisher.publish(ChatStreamPayload.from(3L, sampleMessage()));

		assertThat(result).isFalse();
		verify(redisTemplate).convertAndSend(eq("chat:websocket:broadcast"), anyString());
	}

	private ChatStreamProperties sampleProperties() {
		return new ChatStreamProperties(
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
			false,
			true,
			"chat:websocket:broadcast");
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
