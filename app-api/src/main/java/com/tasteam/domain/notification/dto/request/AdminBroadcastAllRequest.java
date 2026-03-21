package com.tasteam.domain.notification.dto.request;

import java.util.Map;
import java.util.Set;

import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminBroadcastAllRequest(
	@NotNull
	NotificationType notificationType,
	@NotBlank @Size(max = 100)
	String title,
	@NotBlank @Size(max = 500)
	String body,
	@Size(max = 500)
	String deepLink,
	@NotEmpty
	Set<NotificationChannel> channels,
	String templateKey,
	Map<String, Object> templateVariables) {
}
