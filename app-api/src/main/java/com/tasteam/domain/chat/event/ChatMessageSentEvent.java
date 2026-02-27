package com.tasteam.domain.chat.event;

import java.time.Instant;

import com.tasteam.domain.chat.type.ChatMessageType;

public record ChatMessageSentEvent(
	Long chatRoomId,
	Long messageId,
	Long senderId,
	String senderNickname,
	ChatMessageType messageType,
	String preview,
	Instant createdAt) {
}
