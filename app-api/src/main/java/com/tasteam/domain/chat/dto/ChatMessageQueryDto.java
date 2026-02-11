package com.tasteam.domain.chat.dto;

import java.time.Instant;

import com.tasteam.domain.chat.type.ChatMessageType;

public record ChatMessageQueryDto(
	Long id,
	Long memberId,
	String memberNickname,
	String memberProfileImageUrl,
	String content,
	ChatMessageType messageType,
	Instant createdAt) {
}
