package com.tasteam.domain.analytics.resilience;

public record UserActivitySourceOutboxSummary(
	long pendingCount,
	long failedCount,
	long publishedCount,
	long maxRetryCount) {
}
