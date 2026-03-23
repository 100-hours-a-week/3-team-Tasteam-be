package com.tasteam.domain.notification.event;

import java.util.Map;
import java.util.Set;

import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;

public record AdminBroadcastRequestedEvent(
	NotificationType notificationType,
	String title,
	String body,
	String deepLink,
	Set<NotificationChannel> channels,
	String templateKey,
	Map<String, Object> templateVariables) {
}
