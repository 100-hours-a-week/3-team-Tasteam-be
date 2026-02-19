package com.tasteam.domain.analytics.dispatch;

import java.time.Instant;

public record UserActivityDispatchOutboxEntry(
	long id,
	String eventId,
	UserActivityDispatchTarget dispatchTarget,
	String payloadJson,
	UserActivityDispatchOutboxStatus status,
	int retryCount,
	Instant nextRetryAt) {
}
