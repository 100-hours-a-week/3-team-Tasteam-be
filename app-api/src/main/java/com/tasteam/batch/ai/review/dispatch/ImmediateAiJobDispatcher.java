package com.tasteam.batch.ai.review.dispatch;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.review.service.ReviewAnalysisJobExecuteService;
import com.tasteam.batch.ai.service.AiJobClaimService;
import com.tasteam.batch.ai.service.AiJobDispatcher;

/**
 * 리뷰 분석 Job을 즉시 전달: 선점 후 스레드 풀에서 실행.
 * 비동기 전달 도입 시 AsyncAiJobDispatcher로 교체.
 */
@Component("reviewAnalysisJobDispatcher")
public class ImmediateAiJobDispatcher implements AiJobDispatcher {

	private final AiJobClaimService aiJobClaimService;
	private final ReviewAnalysisJobExecuteService jobExecuteService;
	private final Executor reviewAnalysisExecutor;

	public ImmediateAiJobDispatcher(AiJobClaimService aiJobClaimService,
		ReviewAnalysisJobExecuteService jobExecuteService,
		@Qualifier("reviewAnalysisExecutor")
		Executor reviewAnalysisExecutor) {
		this.aiJobClaimService = aiJobClaimService;
		this.jobExecuteService = jobExecuteService;
		this.reviewAnalysisExecutor = reviewAnalysisExecutor;
	}

	@Override
	public void dispatch(long jobId) {
		if (!aiJobClaimService.tryClaimToRunning(jobId)) {
			return;
		}
		reviewAnalysisExecutor.execute(() -> jobExecuteService.execute(jobId));
	}
}
