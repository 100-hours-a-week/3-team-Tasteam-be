package com.tasteam.batch.ai.vector.worker;

public interface VectorUploadBatchWorker {

	/**
	 * 해당 배치 실행에 속한 PENDING VECTOR_UPLOAD Job을 가져와 처리한다.
	 *
	 * @param batchExecutionId 배치 실행 ID (batch_execution.id)
	 */
	void processPendingJobs(long batchExecutionId);
}
