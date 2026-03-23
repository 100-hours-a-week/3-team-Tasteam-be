package com.tasteam.domain.notification.dto.request;

import java.util.List;
import java.util.Map;

import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminMqTestNotificationRequest(
	@NotNull @Positive
	Long recipientId,
	@NotNull
	NotificationType notificationType,
	@NotEmpty
	List<NotificationChannel> channels,
	@NotNull @Size(max = 100)
	String title,
	@NotNull @Size(max = 500)
	String body,
	@Size(max = 500)
	String deepLink,
	Map<String, Object> templateVariables) {
}
