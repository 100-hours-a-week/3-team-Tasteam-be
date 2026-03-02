package com.tasteam.domain.chat.dto.response;

import java.util.List;

public record ChatMessageListResponse(
	Meta meta,
	List<ChatMessageItemResponse> data,
	Page page) {

	public record Meta(Long lastReadMessageId) {
	}

	public record Page(String nextCursor, String afterCursor, int size, boolean hasNext) {
	}
}
