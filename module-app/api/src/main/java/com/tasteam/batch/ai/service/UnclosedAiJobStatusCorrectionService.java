package com.tasteam.batch.ai.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.entity.BatchExecutionStatus;
import com.tasteam.domain.batch.entity.BatchType;
import com.tasteam.domain.batch.repository.AiJobRepository;
import com.tasteam.domain.batch.repository.BatchExecutionRepository;
import com.tasteam.domain.restaurant.repository.RestaurantComparisonRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSentimentRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSummaryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 미종료 AI Job/실행 상태 보정.
 * 1) RUNNING Job 중 이미 결과가 있는 것은 COMPLETED로 보정.
 * 2) 남은 미종료 PENDING/RUNNING Job → FAILED 처리.
 * 3) 미종료 BatchExecution → FAILED 처리.
 * 감정·요약: result.vector_epoch == job.baseEpoch 이면 COMPLETED. 비교: 해당 레스토랑 비교 결과 존재 시 COMPLETED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnclosedAiJobStatusCorrectionService {

	private final AiJobRepository aiJobRepository;
	private final BatchExecutionRepository batchExecutionRepository;
	private final RestaurantReviewSentimentRepository sentimentRepository;
	private final RestaurantReviewSummaryRepository summaryRepository;
	private final RestaurantComparisonRepository comparisonRepository;

	/**
	 * 해당 배치 타입에 대해 미종료 Job/실행 상태를 보정한다.
	 *
	 * @param batchType REVIEW_ANALYSIS_DAILY 또는 RESTAURANT_COMPARISON_WEEKLY
	 * @return 보정 결과 (보정 완료 수, FAILED 처리된 Job 수, FAILED 처리된 실행 수)
	 */
	@Transactional
	public CorrectionResult run(BatchType batchType) {
		Instant now = Instant.now();

		int correctedCount = correctRunningJobsWithResult(batchType);
		if (correctedCount > 0) {
			log.info("Unclosed correction ({}): corrected {} RUNNING job(s) to COMPLETED", batchType, correctedCount);
		}

		int unclosedJobCount = aiJobRepository.markUnclosedJobsAsFailed(
			batchType, AiJobStatus.PENDING, AiJobStatus.RUNNING, AiJobStatus.FAILED);
		if (unclosedJobCount > 0) {
			log.info("Unclosed correction ({}): marked {} unclosed PENDING/RUNNING job(s) as FAILED",
				batchType, unclosedJobCount);
		}

		int unclosedExecutionCount = batchExecutionRepository.markUnclosedBatchExecutionsAsFailed(
			batchType, BatchExecutionStatus.FAILED, now);
		if (unclosedExecutionCount > 0) {
			log.info("Unclosed correction ({}): marked {} unclosed BatchExecution(s) as FAILED",
				batchType, unclosedExecutionCount);
		}

		return new CorrectionResult(correctedCount, unclosedJobCount, unclosedExecutionCount);
	}

	private int correctRunningJobsWithResult(BatchType batchType) {
		List<AiJob> running = aiJobRepository.findByBatchTypeAndUnclosedExecutionAndStatus(
			batchType, AiJobStatus.RUNNING);
		if (running.isEmpty()) {
			return 0;
		}

		List<AiJob> toComplete = new ArrayList<>();
		for (AiJob job : running) {
			if (hasResultForJob(job)) {
				toComplete.add(job);
			}
		}

		if (!toComplete.isEmpty()) {
			toComplete.forEach(AiJob::markCompleted);
			aiJobRepository.saveAll(toComplete);
		}
		return toComplete.size();
	}

	private boolean hasResultForJob(AiJob job) {
		return switch (job.getJobType()) {
			case REVIEW_SENTIMENT -> sentimentRepository.existsByRestaurantIdAndVectorEpoch(
				job.getRestaurantId(), job.getBaseEpoch());
			case REVIEW_SUMMARY -> summaryRepository.existsByRestaurantIdAndVectorEpoch(
				job.getRestaurantId(), job.getBaseEpoch());
			case RESTAURANT_COMPARISON -> comparisonRepository.findByRestaurantId(job.getRestaurantId()).isPresent();
			case VECTOR_UPLOAD -> false;
		};
	}

	public record CorrectionResult(int correctedCount, int unclosedJobCount, int unclosedExecutionCount) {
		public boolean hasRecovered() {
			return correctedCount > 0 || unclosedJobCount > 0 || unclosedExecutionCount > 0;
		}
	}
}
