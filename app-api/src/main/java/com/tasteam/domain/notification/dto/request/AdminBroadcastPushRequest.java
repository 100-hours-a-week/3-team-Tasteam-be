package com.tasteam.domain.notification.dto.request;

import com.tasteam.domain.notification.entity.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminBroadcastPushRequest(
	@NotNull
	NotificationType notificationType,
	@NotBlank @Size(max = 100)
	String title,
	@NotBlank @Size(max = 1000)
	String body,
	@Size(max = 500)
	String deepLink) {
}
