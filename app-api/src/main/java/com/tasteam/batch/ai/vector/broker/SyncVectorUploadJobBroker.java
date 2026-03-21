package com.tasteam.batch.ai.vector.broker;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.service.AiJobBroker;
import com.tasteam.batch.ai.service.AiJobClaimService;
import com.tasteam.batch.ai.vector.worker.VectorUploadJobWorker;

/**
 * 벡터 업로드 Job을 동기 전송: 선점 후 스레드 풀에서 실행.
 */
@Component("syncVectorUploadJobBroker")
public class SyncVectorUploadJobBroker implements AiJobBroker {

	private final AiJobClaimService aiJobClaimService;
	private final VectorUploadJobWorker vectorUploadJobWorker;
	private final Executor vectorUploadExecutor;

	public SyncVectorUploadJobBroker(AiJobClaimService aiJobClaimService,
		VectorUploadJobWorker vectorUploadJobWorker,
		@Qualifier("vectorUploadExecutor")
		Executor vectorUploadExecutor) {
		this.aiJobClaimService = aiJobClaimService;
		this.vectorUploadJobWorker = vectorUploadJobWorker;
		this.vectorUploadExecutor = vectorUploadExecutor;
	}

	@Override
	public void publish(long jobId) {
		if (!aiJobClaimService.tryClaimToRunning(jobId)) {
			return;
		}
		vectorUploadExecutor.execute(() -> vectorUploadJobWorker.execute(jobId));
	}
}
