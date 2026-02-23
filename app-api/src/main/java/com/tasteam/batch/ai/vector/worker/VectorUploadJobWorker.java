package com.tasteam.batch.ai.vector.worker;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import com.tasteam.batch.ai.review.service.ReviewAnalysisJobProducer;
import com.tasteam.batch.ai.vector.service.VectorUploadAiInvokeService;
import com.tasteam.batch.ai.vector.service.VectorUploadDataLoadService;
import com.tasteam.batch.ai.vector.service.VectorUploadDataLoadService.RestaurantWithReviews;
import com.tasteam.batch.ai.vector.service.VectorUploadEpochSyncService;
import com.tasteam.batch.ai.vector.service.VectorUploadInvokeResult;
import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.repository.AiJobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 선점된 한 건의 벡터 업로드 Job 실행: 데이터 조회 → AI 호출 → 성공/실패 분기.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorUploadJobWorker {

	private final VectorUploadDataLoadService dataLoadService;
	private final VectorUploadAiInvokeService aiInvokeService;
	private final VectorUploadEpochSyncService epochSyncService;
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
	 * Job 한 건 실행. 데이터 없으면 스킵(분기 없음), 있으면 AI 호출 후 성공/실패 분기만 수행.
	 */
	public void execute(AiJob job) {
		Optional<RestaurantWithReviews> dataOpt = dataLoadService.loadByRestaurantId(job.getRestaurantId());
		if (dataOpt.isEmpty()) {
			log.warn("Vector upload job skipped: restaurant not found or deleted, jobId={}, restaurantId={}",
				job.getId(), job.getRestaurantId());
			transactionTemplate.executeWithoutResult(__ -> {
				job.markStale();
				aiJobRepository.save(job);
			});
			return;
		}

		RestaurantWithReviews data = dataOpt.get();
		VectorUploadInvokeResult result = aiInvokeService.invoke(data);

		switch (result) {
			case VectorUploadInvokeResult.Success success -> onSuccess(job, data, success);
			case VectorUploadInvokeResult.Failure failure -> onFailure(job, failure);
		}
	}

	private void onSuccess(AiJob job, RestaurantWithReviews data, VectorUploadInvokeResult.Success success) {
		boolean synced = epochSyncService.syncEpochAfterUpload(job, data, Instant.now());
		if (synced) {
			reviewAnalysisJobProducer.createJobsAfterVectorUpload(
				job.getRestaurantId(), job.getBaseEpoch() + 1);

			transactionTemplate.executeWithoutResult(__ -> {
				job.markCompleted();
				aiJobRepository.save(job);
			});
		} else {
			log.debug("Vector upload epoch sync skipped (concurrent), jobId={}, restaurantId={}",
				job.getId(), job.getRestaurantId());
		}
	}

	private void onFailure(AiJob job, VectorUploadInvokeResult.Failure failure) {
		log.warn("Vector upload job failed: jobId={}, restaurantId={}, cause={}",
			job.getId(), job.getRestaurantId(), failure.cause().getMessage());
		transactionTemplate.executeWithoutResult(__ -> {
			job.markFailed();
			aiJobRepository.save(job);
		});
	}
}
