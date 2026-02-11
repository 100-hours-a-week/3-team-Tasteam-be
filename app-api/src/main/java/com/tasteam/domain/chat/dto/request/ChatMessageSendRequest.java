package com.tasteam.domain.chat.dto.request;

import com.tasteam.domain.chat.type.ChatMessageType;

public record ChatMessageSendRequest(
	ChatMessageType messageType,
	String content) {
}
