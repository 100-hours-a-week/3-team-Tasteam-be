package com.tasteam.domain.chat.stream;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.StringUtils;

public record ChatWsBroadcastEvent(
	Long chatRoomId,
	Map<String, String> payload,
	Instant publishedAt,
	String sourceInstance) {

	public static ChatWsBroadcastEvent from(ChatStreamPayload payload, String sourceInstance) {
		return new ChatWsBroadcastEvent(
			payload.chatRoomId(),
			new HashMap<>(payload.toMap()),
			Instant.now(),
			sourceInstance);
	}

	public Long resolveChatRoomId() {
		if (chatRoomId != null) {
			return chatRoomId;
		}
		if (payload == null) {
			return null;
		}
		String chatRoomIdValue = payload.get("chatRoomId");
		if (!StringUtils.hasText(chatRoomIdValue) || "null".equalsIgnoreCase(chatRoomIdValue.trim())) {
			return null;
		}
		return Long.valueOf(chatRoomIdValue);
	}

	public ChatStreamPayload toPayload() {
		return ChatStreamPayload.fromMap(payload == null ? Map.of() : payload);
	}
}
