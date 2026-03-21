package com.tasteam.infra.messagequeue;

import com.tasteam.domain.analytics.resilience.UserActivityReplayResult;

public record UserActivityReplayResponse(
	int processedCount,
	int successCount,
	int failedCount) {

	public static UserActivityReplayResponse from(UserActivityReplayResult result) {
		return new UserActivityReplayResponse(
			result.processedCount(),
			result.successCount(),
			result.failedCount());
	}
}
