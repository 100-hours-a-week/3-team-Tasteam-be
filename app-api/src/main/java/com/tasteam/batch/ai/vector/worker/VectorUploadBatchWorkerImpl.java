package com.tasteam.batch.ai.vector.worker;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.tasteam.batch.ai.service.AiJobDispatcher;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.repository.AiJobRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * VECTOR_UPLOAD_DAILY 배치 실행의 미전달 PENDING Job 보정: 1회 조회 후 각각 디스패처로 전달.
 */
@Slf4j
@Component
public class VectorUploadBatchWorkerImpl implements VectorUploadBatchWorker {

	private final AiJobRepository aiJobRepository;
	private final AiJobDispatcher vectorUploadJobDispatcher;

	public VectorUploadBatchWorkerImpl(AiJobRepository aiJobRepository,
		@Qualifier("vectorUploadJobDispatcher")
		AiJobDispatcher vectorUploadJobDispatcher) {
		this.aiJobRepository = aiJobRepository;
		this.vectorUploadJobDispatcher = vectorUploadJobDispatcher;
	}

	@Override
	public void processPendingJobs(long batchExecutionId) {
		List<AiJob> pending = aiJobRepository.findByBatchExecutionIdAndStatusOrderByCreatedAtAsc(
			batchExecutionId, AiJobStatus.PENDING);
		if (pending.isEmpty()) {
			return;
		}
		log.info("Vector upload batch: dispatching {} pending job(s) for batchExecutionId={}",
			pending.size(), batchExecutionId);
		for (AiJob job : pending) {
			vectorUploadJobDispatcher.dispatch(job.getId());
		}
	}
}
