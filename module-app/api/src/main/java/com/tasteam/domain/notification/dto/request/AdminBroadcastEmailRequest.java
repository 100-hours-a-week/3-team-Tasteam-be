package com.tasteam.domain.notification.dto.request;

import java.util.Map;

import com.tasteam.domain.notification.entity.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminBroadcastEmailRequest(
	@NotNull
	NotificationType notificationType,
	@NotBlank
	String templateKey,
	Map<String, Object> variables) {
}
