package com.tasteam.domain.notification.dto.response;

public record AdminPushNotificationResponse(
	int successCount,
	int failureCount,
	int invalidTokenCount) {
}
