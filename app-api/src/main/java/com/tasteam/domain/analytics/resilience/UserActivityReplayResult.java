package com.tasteam.domain.analytics.resilience;

public record UserActivityReplayResult(
	int processedCount,
	int successCount,
	int failedCount) {
}
