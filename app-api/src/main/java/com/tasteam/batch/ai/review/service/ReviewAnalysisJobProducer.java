package com.tasteam.batch.ai.review.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tasteam.batch.ai.service.AiJobBroker;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobType;
import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.AiJobRepository;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 벡터 업로드 성공 시 해당 레스토랑에 대한 리뷰 분석 Job(감정·요약) 2건 생성 후 브로커로 전달.
 */
@Slf4j
@Service
public class ReviewAnalysisJobProducer {

	private static final BatchType BATCH_TYPE = BatchType.REVIEW_ANALYSIS_DAILY;

	private final BatchExecutionRepository batchExecutionRepository;
	private final AiJobRepository aiJobRepository;
	private final AiJobBroker reviewAnalysisJobBroker;

	public ReviewAnalysisJobProducer(BatchExecutionRepository batchExecutionRepository,
		AiJobRepository aiJobRepository,
		@Qualifier("syncReviewAnalysisJobBroker")
		AiJobBroker reviewAnalysisJobBroker) {
		this.batchExecutionRepository = batchExecutionRepository;
		this.aiJobRepository = aiJobRepository;
		this.reviewAnalysisJobBroker = reviewAnalysisJobBroker;
	}

	/**
	 * 현재 REVIEW_ANALYSIS_DAILY RUNNING 실행을 찾아, 해당 레스토랑에 대한 감정·요약 Job 2건 생성.
	 * 실행이 없으면 로그 후 생성하지 않음.
	 *
	 * @param restaurantId 레스토랑 ID
	 * @param baseEpoch     방금 갱신된 vector_epoch (에폭 싱크 성공 직후 값)
	 */
	@Transactional
	public void createJobsAfterVectorUpload(Long restaurantId, long baseEpoch) {
		Optional<BatchExecution> executionOpt = batchExecutionRepository
			.findByBatchTypeAndStatus(BATCH_TYPE, BatchExecutionStatus.RUNNING);
		if (executionOpt.isEmpty()) {
			log.warn("REVIEW_ANALYSIS_DAILY RUNNING execution not found; skipping job creation for restaurantId={}",
				restaurantId);
			return;
		}
		BatchExecution execution = executionOpt.get();

		// 첫 Job 생성 시점 기록
		if (aiJobRepository.countByBatchExecutionId(execution.getId()) == 0) {
			execution.recordWorkStarted(Instant.now());
			batchExecutionRepository.save(execution);
		}

		List<AiJob> jobs = List.of(
			AiJob.create(execution, AiJobType.REVIEW_SENTIMENT, restaurantId, baseEpoch),
			AiJob.create(execution, AiJobType.REVIEW_SUMMARY, restaurantId, baseEpoch));
		aiJobRepository.saveAll(jobs);
		List<Long> jobIds = jobs.stream().map(AiJob::getId).toList();
		long execId = execution.getId();
		// Job 커밋 완료 후 publish해야 broker의 claim이 row를 조회할 수 있음
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				for (Long jobId : jobIds) {
					reviewAnalysisJobBroker.publish(jobId);
				}
				log.info(
					"Review analysis jobs created and dispatched: restaurantId={}, batchExecutionId={}, baseEpoch={}",
					restaurantId, execId, baseEpoch);
			}
		});
	}
}
