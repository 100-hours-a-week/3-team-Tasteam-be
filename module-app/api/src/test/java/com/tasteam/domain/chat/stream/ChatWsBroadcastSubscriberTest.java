package com.tasteam.domain.chat.stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.type.ChatMessageType;

@UnitTest
@DisplayName("[유닛](Chat) ChatWsBroadcastSubscriber 단위 테스트")
class ChatWsBroadcastSubscriberTest {

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Test
	@DisplayName("유효한 Pub/Sub 이벤트를 수신하면 /topic/chat-rooms/{id}로 브로드캐스트한다")
	void onMessage_success() throws Exception {
		SimpMessagingTemplate messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
		ChatWsBroadcastSubscriber subscriber = new ChatWsBroadcastSubscriber(messagingTemplate, objectMapper);
		ChatWsBroadcastEvent event = ChatWsBroadcastEvent.from(ChatStreamPayload.from(3L, sampleMessage()),
			"instance-a");
		Message message = new DefaultMessage(
			"chat:websocket:broadcast".getBytes(StandardCharsets.UTF_8),
			objectMapper.writeValueAsBytes(event));

		subscriber.onMessage(message, null);

		verify(messagingTemplate).convertAndSend(eq("/topic/chat-rooms/3"), any(ChatMessageItemResponse.class));
	}

	@Test
	@DisplayName("chatRoomId가 없으면 브로드캐스트하지 않는다")
	void onMessage_missingRoomId() throws Exception {
		SimpMessagingTemplate messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
		ChatWsBroadcastSubscriber subscriber = new ChatWsBroadcastSubscriber(messagingTemplate, objectMapper);
		ChatWsBroadcastEvent event = new ChatWsBroadcastEvent(null, Map.of("chatRoomId", "null"), Instant.now(),
			"instance-a");
		Message message = new DefaultMessage(
			"chat:websocket:broadcast".getBytes(StandardCharsets.UTF_8),
			objectMapper.writeValueAsBytes(event));

		subscriber.onMessage(message, null);

		verifyNoInteractions(messagingTemplate);
	}

	@Test
	@DisplayName("역직렬화 실패 시 브로드캐스트하지 않는다")
	void onMessage_deserializeFail() {
		SimpMessagingTemplate messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
		ChatWsBroadcastSubscriber subscriber = new ChatWsBroadcastSubscriber(messagingTemplate, objectMapper);
		Message invalidMessage = new DefaultMessage(
			"chat:websocket:broadcast".getBytes(StandardCharsets.UTF_8),
			"not-json".getBytes(StandardCharsets.UTF_8));

		subscriber.onMessage(invalidMessage, null);

		verifyNoInteractions(messagingTemplate);
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
