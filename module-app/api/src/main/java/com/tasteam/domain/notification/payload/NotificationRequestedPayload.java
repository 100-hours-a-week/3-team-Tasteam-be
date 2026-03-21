package com.tasteam.domain.notification.payload;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;

public record NotificationRequestedPayload(
	String eventId,
	String eventType,
	Long recipientId,
	NotificationType notificationType,
	List<NotificationChannel> channels,
	String templateKey,
	Map<String, Object> templateVariables,
	String deepLink,
	Instant occurredAt) {
}
