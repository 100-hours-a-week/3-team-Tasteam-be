package com.tasteam.batch.ai.review.worker;

/**
 * REVIEW_ANALYSIS_DAILY 배치 실행에 속한 PENDING Job(감정·요약)을 소비하는 워커.
 */
public interface ReviewAnalysisBatchWorker {

	/**
	 * 해당 배치 실행에 속한 PENDING REVIEW_SENTIMENT·REVIEW_SUMMARY Job을 가져와 처리한다.
	 *
	 * @param batchExecutionId 배치 실행 ID (batch_execution.id)
	 */
	void processPendingJobs(long batchExecutionId);
}
