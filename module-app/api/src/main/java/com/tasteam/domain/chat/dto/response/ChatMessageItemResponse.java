package com.tasteam.domain.chat.dto.response;

import java.time.Instant;
import java.util.List;

import com.tasteam.domain.chat.type.ChatMessageType;

public record ChatMessageItemResponse(
	Long id,
	Long memberId,
	String memberNickname,
	String memberProfileImageUrl,
	String content,
	ChatMessageType messageType,
	List<ChatMessageFileItemResponse> files,
	Instant createdAt) {
}
