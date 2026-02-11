package com.tasteam.domain.chat.dto.response;

import java.time.Instant;

import com.tasteam.domain.chat.type.ChatMessageType;

public record ChatMessageSendResponse(
	Long id,
	ChatMessageType messageType,
	String content,
	Object image,
	Instant createdAt) {
}
