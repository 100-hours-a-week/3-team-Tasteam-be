package com.tasteam.batch.ai.vector.dispatch;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.service.AiJobClaimService;
import com.tasteam.batch.ai.service.AiJobDispatcher;
import com.tasteam.batch.ai.vector.service.VectorUploadJobExecuteService;

/**
 * 벡터 업로드 Job을 즉시 전달: 선점 후 스레드 풀에서 실행.
 * MQ 도입 시 AsyncVectorUploadJobDispatcher로 교체.
 */
@Component("vectorUploadJobDispatcher")
public class ImmediateVectorUploadJobDispatcher implements AiJobDispatcher {

	private final AiJobClaimService aiJobClaimService;
	private final VectorUploadJobExecuteService jobExecuteService;
	private final Executor vectorUploadExecutor;

	public ImmediateVectorUploadJobDispatcher(AiJobClaimService aiJobClaimService,
		VectorUploadJobExecuteService jobExecuteService,
		@Qualifier("vectorUploadExecutor")
		Executor vectorUploadExecutor) {
		this.aiJobClaimService = aiJobClaimService;
		this.jobExecuteService = jobExecuteService;
		this.vectorUploadExecutor = vectorUploadExecutor;
	}

	@Override
	public void dispatch(long jobId) {
		if (!aiJobClaimService.tryClaimToRunning(jobId)) {
			return;
		}
		vectorUploadExecutor.execute(() -> jobExecuteService.execute(jobId));
	}
}
