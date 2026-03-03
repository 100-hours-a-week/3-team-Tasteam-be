package com.tasteam.domain.batch.entity;

/**
 * 배치 실행 전체 상태.
 */
public enum BatchExecutionStatus {
	RUNNING,
	COMPLETED,
	FAILED,
	/** finishTimeout 경과로 PENDING/RUNNING을 강제 FAILED 처리 후 종료한 경우 */
	TIMEOUT
}
