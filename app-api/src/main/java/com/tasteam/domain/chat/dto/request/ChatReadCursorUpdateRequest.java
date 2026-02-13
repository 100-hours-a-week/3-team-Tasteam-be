package com.tasteam.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatReadCursorUpdateRequest(
	@NotNull @Positive
	Long lastReadMessageId) {
}
