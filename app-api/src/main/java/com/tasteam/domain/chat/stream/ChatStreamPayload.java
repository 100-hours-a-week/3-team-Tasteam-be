package com.tasteam.domain.chat.stream;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tasteam.domain.chat.dto.response.ChatMessageFileItemResponse;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.type.ChatMessageFileType;
import com.tasteam.domain.chat.type.ChatMessageType;

public record ChatStreamPayload(
	Long chatRoomId,
	Long messageId,
	Long memberId,
	String memberNickname,
	String memberProfileImageUrl,
	String content,
	ChatMessageType messageType,
	ChatMessageFileType fileType,
	String fileUrl,
	Instant createdAt) {

	public static ChatStreamPayload from(Long chatRoomId, ChatMessageItemResponse item) {
		ChatMessageFileItemResponse file = item.files() == null || item.files().isEmpty()
			? null
			: item.files().get(0);
		return new ChatStreamPayload(
			chatRoomId,
			item.id(),
			item.memberId(),
			item.memberNickname(),
			item.memberProfileImageUrl(),
			item.content(),
			item.messageType(),
			file == null ? null : file.fileType(),
			file == null ? null : file.fileUrl(),
			item.createdAt());
	}

	public static ChatStreamPayload fromMap(Map<String, String> map) {
		String fileTypeValue = map.get("fileType");
		return new ChatStreamPayload(
			parseLong(map.get("chatRoomId")),
			parseLong(map.get("messageId")),
			parseLong(map.get("memberId")),
			map.get("memberNickname"),
			map.get("memberProfileImageUrl"),
			map.get("content"),
			ChatMessageType.valueOf(map.get("messageType")),
			fileTypeValue == null ? null : ChatMessageFileType.valueOf(fileTypeValue),
			map.get("fileUrl"),
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
		map.put("fileType", fileType == null ? null : fileType.name());
		map.put("fileUrl", fileUrl);
		map.put("createdAt", createdAt.toString());
		return map;
	}

	public ChatMessageItemResponse toMessageItem() {
		List<ChatMessageFileItemResponse> files = fileUrl == null
			? List.of()
			: List.of(new ChatMessageFileItemResponse(fileType, fileUrl));
		return new ChatMessageItemResponse(
			messageId,
			memberId,
			memberNickname,
			memberProfileImageUrl,
			content,
			messageType,
			files,
			createdAt);
	}

	private static Long parseLong(String value) {
		if (value == null) {
			return null;
		}
		return Long.valueOf(value);
	}
}
