package com.tasteam.domain.analytics.dispatch;

public record UserActivityDispatchResult(
	int processedCount,
	int successCount,
	int failedCount,
	boolean circuitOpen) {
}
