package com.tasteam.domain.notification.outbox;

import java.time.Instant;

public record NotificationOutboxEntry(
	Long id,
	String eventId,
	String eventType,
	Long recipientId,
	String payloadJson,
	NotificationOutboxStatus status,
	int retryCount,
	Instant nextRetryAt) {
}
