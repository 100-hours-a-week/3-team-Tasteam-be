package com.tasteam.domain.admin.dto.response;

public record AdminJobResponse(
	String jobName,
	int successCount,
	int failedCount,
	int skippedCount) {
}
