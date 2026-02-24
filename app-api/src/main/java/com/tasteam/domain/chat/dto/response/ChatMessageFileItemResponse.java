package com.tasteam.domain.chat.dto.response;

import com.tasteam.domain.chat.type.ChatMessageFileType;

public record ChatMessageFileItemResponse(
	ChatMessageFileType fileType,
	String fileUrl) {
}
