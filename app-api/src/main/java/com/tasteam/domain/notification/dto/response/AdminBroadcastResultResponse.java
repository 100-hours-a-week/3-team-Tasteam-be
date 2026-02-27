package com.tasteam.domain.notification.dto.response;

public record AdminBroadcastResultResponse(
	int totalTargets,
	int successCount,
	int failureCount,
	int skippedCount) {
}
