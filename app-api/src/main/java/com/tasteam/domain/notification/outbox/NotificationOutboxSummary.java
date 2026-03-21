package com.tasteam.domain.notification.outbox;

public record NotificationOutboxSummary(
	long pendingCount,
	long publishedCount,
	long failedCount,
	long retryingCount,
	long maxRetryCount) {
}
