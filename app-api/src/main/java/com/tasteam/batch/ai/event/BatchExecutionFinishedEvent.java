package com.tasteam.batch.ai.event;

import java.time.Instant;

import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;

/**
 * AI 배치 실행 종료 이벤트.
 */
public record BatchExecutionFinishedEvent(
	long batchExecutionId,
	BatchType batchType,
	BatchExecutionStatus status,
	Instant startedAt,
	Instant finishedAt,
	int totalJobs,
	int successCount,
	int failedCount,
	int staleCount) {
}
