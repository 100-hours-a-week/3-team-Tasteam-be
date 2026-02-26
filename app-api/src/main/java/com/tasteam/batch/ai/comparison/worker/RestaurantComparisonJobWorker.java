package com.tasteam.batch.ai.comparison.worker;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.tasteam.batch.ai.review.runner.AnalysisRunResult;
import com.tasteam.batch.ai.review.runner.ReviewAnalysisRunner;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.entity.AiJobType;
import com.tasteam.domain.batch.repository.AiJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 선점된 주간 레스토랑 비교 Job 1건 실행. Runner 호출 후 성공/실패 시 Job 완료·실패 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantComparisonJobWorker {

	private final AiJobRepository aiJobRepository;
	private final ReviewAnalysisRunner reviewAnalysisRunner;
	private final TransactionTemplate transactionTemplate;

	/**
	 * jobId로 Job 조회 후 실행. 없거나 RUNNING이 아니면 스킵.
	 */
	public void execute(long jobId) {
		Optional<AiJob> opt = aiJobRepository.findById(jobId);
		if (opt.isEmpty()) {
			log.warn("Restaurant comparison job not found, skipping: jobId={}", jobId);
			return;
		}
		AiJob job = opt.get();
		if (job.getStatus() != AiJobStatus.RUNNING) {
			log.debug("Restaurant comparison job not RUNNING, skipping: jobId={}, status={}", jobId, job.getStatus());
			return;
		}
		if (job.getJobType() != AiJobType.RESTAURANT_COMPARISON) {
			log.warn("Unexpected job type for comparison worker: jobId={}, type={}", jobId, job.getJobType());
			markFailedInTx(job);
			return;
		}

		long restaurantId = job.getRestaurantId();
		AnalysisRunResult result = reviewAnalysisRunner.runComparisonAnalysis(restaurantId);

		switch (result) {
			case AnalysisRunResult.Success __ -> {
				transactionTemplate.executeWithoutResult(ts -> {
					job.markCompleted();
					aiJobRepository.save(job);
				});
				log.debug("Restaurant comparison job completed: jobId={}, restaurantId={}", jobId, restaurantId);
			}
			case AnalysisRunResult.Failure f -> {
				log.error("Restaurant comparison failed: jobId={}, restaurantId={}", jobId, restaurantId, f.cause());
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
