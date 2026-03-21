package com.tasteam.domain.chat.dto;

import java.time.Instant;

public record ChatRoomMemberSnapshot(
	Long memberId,
	Long lastReadMessageId,
	Instant lastReadUpdatedAt) {
}
