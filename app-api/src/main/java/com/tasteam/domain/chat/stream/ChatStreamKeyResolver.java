package com.tasteam.domain.chat.stream;

import org.springframework.stereotype.Component;

@Component
public class ChatStreamKeyResolver {
	private static final String ROOM_STREAM_PREFIX = "chat:room:";
	private static final String PARTITION_STREAM_PREFIX = "chat:partition:";
	private static final String DEAD_LETTER_STREAM = "chat:dead-letter";

	public String roomStreamKey(Long chatRoomId) {
		return ROOM_STREAM_PREFIX + chatRoomId;
	}

	public String partitionStreamKey(int partitionId) {
		return PARTITION_STREAM_PREFIX + partitionId;
	}

	public int resolvePartition(Long chatRoomId, int partitionCount) {
		if (chatRoomId == null) {
			throw new IllegalArgumentException("chatRoomId must not be null");
		}
		if (partitionCount <= 0) {
			throw new IllegalArgumentException("partitionCount must be greater than 0");
		}
		return (int)Math.floorMod(chatRoomId, (long)partitionCount);
	}

	public String deadLetterStreamKey() {
		return DEAD_LETTER_STREAM;
	}
}
