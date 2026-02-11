package com.tasteam.domain.chat.dto.response;

import java.time.Instant;

public record ChatReadCursorUpdateResponse(
	Long roomId,
	Long memberId,
	Long lastReadMessageId,
	Instant updatedAt) {
}
