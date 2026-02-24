package com.tasteam.batch.ai.vector.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.AiJobRepository;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 벡터 업로드 배치 실행 전 사전 작업
 * 이전 배치에서 미종료된 Job 또는 BatchExecution 실패(FAILED) 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorUploadBatchPreConditionService {

	private static final BatchType BATCH_TYPE = BatchType.VECTOR_UPLOAD_DAILY;

	private final AiJobRepository aiJobRepository;
	private final BatchExecutionRepository batchExecutionRepository;

	@Transactional
	public PreConditionResult runPreCondition() {
		Instant now = Instant.now();

		int unclosedJobCount = aiJobRepository.markPendingAndRunningAsFailedByBatchTypeForUnclosedExecutions(
			BATCH_TYPE, AiJobStatus.PENDING, AiJobStatus.RUNNING, AiJobStatus.FAILED);
		if (unclosedJobCount > 0) {
			log.info("Vector upload pre-condition: marked {} unclosed PENDING/RUNNING job(s) as FAILED",
				unclosedJobCount);
		}

		int unclosedExecutionCount = batchExecutionRepository.markUnclosedAsFailed(
			BATCH_TYPE, BatchExecutionStatus.FAILED, now);
		if (unclosedExecutionCount > 0) {
			log.info("Vector upload pre-condition: marked {} unclosed BatchExecution(s) as FAILED",
				unclosedExecutionCount);
		}

		return new PreConditionResult(unclosedJobCount, unclosedExecutionCount);
	}

	public record PreConditionResult(int unclosedJobCount, int unclosedExecutionCount) {
		public boolean hasRecovered() {
			return unclosedJobCount > 0 || unclosedExecutionCount > 0;
		}
	}
}
