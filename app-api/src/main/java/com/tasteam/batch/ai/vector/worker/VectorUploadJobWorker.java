package com.tasteam.batch.ai.vector.worker;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.tasteam.batch.ai.review.service.ReviewAnalysisJobProducer;
import com.tasteam.batch.ai.vector.runner.VectorUploadRunResult;
import com.tasteam.batch.ai.vector.runner.VectorUploadRunner;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.repository.AiJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 선점된 한 건의 벡터 업로드 Job 실행: Runner 호출 후 성공 시 createJobs + markCompleted, 실패 시 markFailed/Stale.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorUploadJobWorker {

	private final VectorUploadRunner vectorUploadRunner;
	private final ReviewAnalysisJobProducer reviewAnalysisJobProducer;
	private final AiJobRepository aiJobRepository;
	private final TransactionTemplate transactionTemplate;

	/**
	 * jobId로 Job 조회 후 실행. 없거나 RUNNING이 아니면 스킵 (이미 처리됨 또는 상태 이상).
	 */
	public void execute(long jobId) {
		Optional<AiJob> opt = aiJobRepository.findById(jobId);
		if (opt.isEmpty()) {
			log.warn("Vector upload job not found, skipping: jobId={}", jobId);
			return;
		}
		AiJob job = opt.get();
		if (job.getStatus() != AiJobStatus.RUNNING) {
			log.debug("Vector upload job not RUNNING, skipping: jobId={}, status={}", jobId, job.getStatus());
			return;
		}
		execute(job);
	}

	/**
	 * Job 한 건 실행. Runner 호출 후 결과에 따라 createJobs + markCompleted / markFailed / markStale 처리.
	 */
	public void execute(AiJob job) {
		VectorUploadRunResult result = vectorUploadRunner.run(job);

		switch (result) {
			case VectorUploadRunResult.Success s -> {
				reviewAnalysisJobProducer.createJobsAfterVectorUpload(job.getRestaurantId(), s.newVectorEpoch());
				transactionTemplate.executeWithoutResult(ts -> {
					job.markCompleted();
					aiJobRepository.save(job);
				});
			}
			case VectorUploadRunResult.DataMissing __ -> {
				log.warn("Vector upload job skipped: restaurant not found or deleted, jobId={}, restaurantId={}",
					job.getId(), job.getRestaurantId());
				transactionTemplate.executeWithoutResult(ts -> {
					job.markStale();
					aiJobRepository.save(job);
				});
			}
			case VectorUploadRunResult.InvokeFailed f -> {
				log.warn("Vector upload job failed: jobId={}, restaurantId={}, cause={}",
					job.getId(), job.getRestaurantId(), f.cause().getMessage());
				transactionTemplate.executeWithoutResult(ts -> {
					job.markFailed();
					aiJobRepository.save(job);
				});
			}
			case VectorUploadRunResult.SyncSkipped __ -> {
				log.debug("Vector upload epoch sync skipped (concurrent), jobId={}, restaurantId={}",
					job.getId(), job.getRestaurantId());
			}
		}
	}
}
