package com.tasteam.domain.analytics.resilience;

import java.time.Instant;

public record UserActivitySourceOutboxEntry(
	Long id,
	String eventId,
	String payloadJson,
	UserActivitySourceOutboxStatus status,
	Integer retryCount,
	Instant nextRetryAt) {
}
