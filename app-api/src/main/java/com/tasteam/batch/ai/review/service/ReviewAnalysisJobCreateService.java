package com.tasteam.batch.ai.review.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.batch.ai.service.AiJobDispatcher;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobType;
import com.tasteam.domain.batch.entity.BatchExecution;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.AiJobRepository;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 벡터 업로드 성공 시 해당 레스토랑에 대한 리뷰 분석 Job(감정·요약) 2건 생성 후 디스패처로 전달.
 */
@Slf4j
@Service
public class ReviewAnalysisJobCreateService {

	private static final BatchType BATCH_TYPE = BatchType.REVIEW_ANALYSIS_DAILY;

	private final BatchExecutionRepository batchExecutionRepository;
	private final AiJobRepository aiJobRepository;
	private final AiJobDispatcher aiJobDispatcher;

	public ReviewAnalysisJobCreateService(BatchExecutionRepository batchExecutionRepository,
		AiJobRepository aiJobRepository,
		@Qualifier("reviewAnalysisJobDispatcher")
		AiJobDispatcher aiJobDispatcher) {
		this.batchExecutionRepository = batchExecutionRepository;
		this.aiJobRepository = aiJobRepository;
		this.aiJobDispatcher = aiJobDispatcher;
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

		List<AiJob> jobs = List.of(
			AiJob.create(execution, AiJobType.REVIEW_SENTIMENT, restaurantId, baseEpoch),
			AiJob.create(execution, AiJobType.REVIEW_SUMMARY, restaurantId, baseEpoch));
		aiJobRepository.saveAll(jobs);
		for (AiJob job : jobs) {
			aiJobDispatcher.dispatch(job.getId());
		}
		log.info("Review analysis jobs created and dispatched: restaurantId={}, batchExecutionId={}, baseEpoch={}",
			restaurantId, execution.getId(), baseEpoch);
	}
}
