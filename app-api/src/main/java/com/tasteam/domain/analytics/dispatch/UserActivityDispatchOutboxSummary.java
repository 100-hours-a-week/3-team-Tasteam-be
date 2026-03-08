package com.tasteam.domain.analytics.dispatch;

public record UserActivityDispatchOutboxSummary(
	UserActivityDispatchTarget target,
	long pendingCount,
	long failedCount,
	long dispatchedCount,
	long maxRetryCount) {
}
