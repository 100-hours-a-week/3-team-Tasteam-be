package com.tasteam.domain.chat.dto.request;

public record ChatMessageListRequest(Long cursor, Integer size) {
	public int sizeOrDefault(int defaultSize) {
		return size == null ? defaultSize : size;
	}
}
