package com.tasteam.domain.chat.stream;

import org.springframework.stereotype.Component;

@Component
public class ChatStreamKeyResolver {
	private static final String ROOM_STREAM_PREFIX = "chat:room:";
	private static final String DEAD_LETTER_STREAM = "chat:dead-letter";

	public String roomStreamKey(Long chatRoomId) {
		return ROOM_STREAM_PREFIX + chatRoomId;
	}

	public String deadLetterStreamKey() {
		return DEAD_LETTER_STREAM;
	}
}
