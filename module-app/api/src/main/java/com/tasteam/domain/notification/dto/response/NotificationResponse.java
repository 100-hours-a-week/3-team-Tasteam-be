package com.tasteam.domain.notification.dto.response;

import java.time.Instant;

import com.tasteam.domain.notification.entity.Notification;
import com.tasteam.domain.notification.entity.NotificationType;

public record NotificationResponse(
	Long id,
	NotificationType notificationType,
	String title,
	String body,
	String deepLink,
	Instant createdAt,
	Instant readAt) {
	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
			notification.getId(),
			notification.getNotificationType(),
			notification.getTitle(),
			notification.getBody(),
			notification.getDeepLink(),
			notification.getCreatedAt(),
			notification.getReadAt());
	}
}
