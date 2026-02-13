package com.tasteam.domain.chat.stream;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.type.ChatMessageType;

public record ChatStreamPayload(
	Long chatRoomId,
	Long messageId,
	Long memberId,
	String memberNickname,
	String memberProfileImageUrl,
	String content,
	ChatMessageType messageType,
	Instant createdAt) {

	public static ChatStreamPayload from(Long chatRoomId, ChatMessageItemResponse item) {
		return new ChatStreamPayload(
			chatRoomId,
			item.id(),
			item.memberId(),
			item.memberNickname(),
			item.memberProfileImageUrl(),
			item.content(),
			item.messageType(),
			item.createdAt());
	}

	public static ChatStreamPayload fromMap(Map<String, String> map) {
		return new ChatStreamPayload(
			parseLong(map.get("chatRoomId")),
			parseLong(map.get("messageId")),
			parseLong(map.get("memberId")),
			map.get("memberNickname"),
			map.get("memberProfileImageUrl"),
			map.get("content"),
			ChatMessageType.valueOf(map.get("messageType")),
			Instant.parse(map.get("createdAt")));
	}

	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<>();
		map.put("chatRoomId", String.valueOf(chatRoomId));
		map.put("messageId", String.valueOf(messageId));
		map.put("memberId", String.valueOf(memberId));
		map.put("memberNickname", memberNickname);
		map.put("memberProfileImageUrl", memberProfileImageUrl);
		map.put("content", content);
		map.put("messageType", messageType.name());
		map.put("createdAt", createdAt.toString());
		return map;
	}

	public ChatMessageItemResponse toMessageItem() {
		return new ChatMessageItemResponse(
			messageId,
			memberId,
			memberNickname,
			memberProfileImageUrl,
			content,
			messageType,
			createdAt);
	}

	private static Long parseLong(String value) {
		if (value == null) {
			return null;
		}
		return Long.valueOf(value);
	}
}
