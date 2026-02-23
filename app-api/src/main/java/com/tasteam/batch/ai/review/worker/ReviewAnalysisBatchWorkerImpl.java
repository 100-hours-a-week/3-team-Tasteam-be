package com.tasteam.batch.ai.review.worker;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.service.AiJobDispatcher;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.repository.AiJobRepository;

/**
 * REVIEW_ANALYSIS_DAILY 배치 실행의 미전달 PENDING Job 보정: 1회 조회 후 각각 디스패처로 전달.
 * 정상 경로는 Job 생성 직후 디스패처 호출(B-2-1). 이 메서드는 재기동 등으로 전달 누락된 Job이 있을 때만 사용.
 */
@Component
public class ReviewAnalysisBatchWorkerImpl implements ReviewAnalysisBatchWorker {

	private final AiJobRepository aiJobRepository;
	private final AiJobDispatcher aiJobDispatcher;

	public ReviewAnalysisBatchWorkerImpl(AiJobRepository aiJobRepository,
		@Qualifier("reviewAnalysisJobDispatcher")
		AiJobDispatcher aiJobDispatcher) {
		this.aiJobRepository = aiJobRepository;
		this.aiJobDispatcher = aiJobDispatcher;
	}

	@Override
	public void processPendingJobs(long batchExecutionId) {
		List<AiJob> pending = aiJobRepository.findByBatchExecutionIdAndStatusOrderByCreatedAtAsc(
			batchExecutionId, AiJobStatus.PENDING);
		for (AiJob job : pending) {
			aiJobDispatcher.dispatch(job.getId());
		}
	}
}
