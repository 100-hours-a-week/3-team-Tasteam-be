package com.tasteam.batch.ai.review.broker;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.review.worker.ReviewAnalysisJobWorker;
import com.tasteam.batch.ai.service.AiJobBroker;
import com.tasteam.batch.ai.service.AiJobClaimService;

/**
 * 리뷰 분석 Job을 동기 전송: 선점 후 스레드 풀에서 실행.
 */
@Component("syncReviewAnalysisJobBroker")
public class SyncReviewAnalysisJobBroker implements AiJobBroker {

	private final AiJobClaimService aiJobClaimService;
	private final ReviewAnalysisJobWorker reviewAnalysisJobWorker;
	private final Executor reviewAnalysisExecutor;

	public SyncReviewAnalysisJobBroker(AiJobClaimService aiJobClaimService,
		ReviewAnalysisJobWorker reviewAnalysisJobWorker,
		@Qualifier("reviewAnalysisExecutor")
		Executor reviewAnalysisExecutor) {
		this.aiJobClaimService = aiJobClaimService;
		this.reviewAnalysisJobWorker = reviewAnalysisJobWorker;
		this.reviewAnalysisExecutor = reviewAnalysisExecutor;
	}

	@Override
	public void publish(long jobId) {
		if (!aiJobClaimService.tryClaimToRunning(jobId)) {
			return;
		}
		reviewAnalysisExecutor.execute(() -> reviewAnalysisJobWorker.execute(jobId));
	}
}
