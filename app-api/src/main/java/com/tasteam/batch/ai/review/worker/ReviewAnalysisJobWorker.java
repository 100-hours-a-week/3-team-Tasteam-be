package com.tasteam.batch.ai.review.worker;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.tasteam.batch.ai.review.runner.AnalysisRunResult;
import com.tasteam.batch.ai.review.runner.ReviewAnalysisRunner;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.repository.AiJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 선점된 리뷰 분석 Job 1건 실행 (감정 또는 요약). Runner 호출 후 성공/실패 시 Job 완료·실패 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewAnalysisJobWorker {

	private final AiJobRepository aiJobRepository;
	private final ReviewAnalysisRunner reviewAnalysisRunner;
	private final TransactionTemplate transactionTemplate;

	/**
	 * jobId로 Job 조회 후 실행. 없거나 RUNNING이 아니면 스킵(이미 처리됨 또는 상태 이상).
	 */
	public void execute(long jobId) {
		Optional<AiJob> opt = aiJobRepository.findById(jobId);
		if (opt.isEmpty()) {
			log.warn("Review analysis job not found, skipping: jobId={}", jobId);
			return;
		}
		AiJob job = opt.get();
		if (job.getStatus() != AiJobStatus.RUNNING) {
			log.debug("Review analysis job not RUNNING, skipping: jobId={}, status={}", jobId, job.getStatus());
			return;
		}

		long restaurantId = job.getRestaurantId();
		long baseEpoch = job.getBaseEpoch();

		AnalysisRunResult result = switch (job.getJobType()) {
			case REVIEW_SENTIMENT -> reviewAnalysisRunner.runSentimentAnalysis(restaurantId, baseEpoch);
			case REVIEW_SUMMARY -> reviewAnalysisRunner.runSummaryAnalysis(restaurantId, baseEpoch);
			case VECTOR_UPLOAD, RESTAURANT_COMPARISON -> {
				log.warn("Unsupported job type for review analysis: jobId={}, type={}", job.getId(),
					job.getJobType());
				yield new AnalysisRunResult.Failure(new IllegalArgumentException("Unsupported job type"));
			}
		};

		switch (result) {
			case AnalysisRunResult.Success __ -> transactionTemplate.executeWithoutResult(ts -> {
				job.markCompleted();
				aiJobRepository.save(job);
			});
			case AnalysisRunResult.Failure f -> {
				log.error("Review analysis failed: jobId={}, restaurantId={}, type={}",
					job.getId(), restaurantId, job.getJobType(), f.cause());
				markFailedInTx(job);
			}
		}
	}

	private void markFailedInTx(AiJob job) {
		transactionTemplate.executeWithoutResult(ts -> {
			job.markFailed();
			aiJobRepository.save(job);
		});
	}
}
