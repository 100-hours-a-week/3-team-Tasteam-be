package com.tasteam.domain.chat.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.type.ChatMessageType;

@UnitTest
@DisplayName("[유닛](Chat) ChatWsBroadcastEvent 단위 테스트")
class ChatWsBroadcastEventTest {

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Test
	@DisplayName("이벤트는 JSON 직렬화/역직렬화 후 payload를 복원할 수 있다")
	void serializeDeserialize_success() throws Exception {
		ChatStreamPayload payload = ChatStreamPayload.from(3L, sampleMessage());
		ChatWsBroadcastEvent event = ChatWsBroadcastEvent.from(payload, "instance-a");

		String json = objectMapper.writeValueAsString(event);
		ChatWsBroadcastEvent restored = objectMapper.readValue(json, ChatWsBroadcastEvent.class);

		assertThat(restored.resolveChatRoomId()).isEqualTo(3L);
		assertThat(restored.toPayload().chatRoomId()).isEqualTo(3L);
		assertThat(restored.toPayload().messageId()).isEqualTo(101L);
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
