package com.tasteam.infra.messagequeue;

import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxSummary;

public record UserActivityOutboxSummaryResponse(
	long pendingCount,
	long failedCount,
	long publishedCount,
	long maxRetryCount) {

	public static UserActivityOutboxSummaryResponse from(UserActivitySourceOutboxSummary summary) {
		return new UserActivityOutboxSummaryResponse(
			summary.pendingCount(),
			summary.failedCount(),
			summary.publishedCount(),
			summary.maxRetryCount());
	}
}
