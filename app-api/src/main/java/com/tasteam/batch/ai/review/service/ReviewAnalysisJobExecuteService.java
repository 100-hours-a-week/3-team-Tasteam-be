package com.tasteam.batch.ai.review.service;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.tasteam.domain.batch.entity.AiJob;
import com.tasteam.domain.batch.entity.AiJobStatus;
import com.tasteam.domain.batch.repository.AiJobRepository;
import com.tasteam.domain.restaurant.entity.RestaurantReviewSentiment;
import com.tasteam.domain.restaurant.entity.RestaurantReviewSummary;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSentimentRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSummaryRepository;
import com.tasteam.infra.ai.AiClient;
import com.tasteam.infra.ai.dto.AiSentimentAnalysisResponse;
import com.tasteam.infra.ai.dto.AiSentimentBatchRequest;
import com.tasteam.infra.ai.dto.AiSentimentBatchResponse;
import com.tasteam.infra.ai.dto.AiSummaryBatchRequest;
import com.tasteam.infra.ai.dto.AiSummaryBatchResponse;
import com.tasteam.infra.ai.dto.AiSummaryDisplayResponse;
import com.tasteam.infra.ai.exception.AiServerException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 선점된 리뷰 분석 Job 1건 실행 (감정 또는 요약).
 * B-2-5~B-2-7: 결과 저장·COMPLETED/FAILED 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewAnalysisJobExecuteService {

	private final AiJobRepository aiJobRepository;
	private final AiClient aiClient;
	private final TransactionTemplate transactionTemplate;
	private final RestaurantReviewSentimentRepository sentimentRepository;
	private final RestaurantReviewSummaryRepository summaryRepository;

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
		try {
			switch (job.getJobType()) {
				case REVIEW_SENTIMENT -> {
					AiSentimentBatchRequest request = AiSentimentBatchRequest.singleRestaurant(restaurantId);
					AiSentimentBatchResponse response = aiClient.analyzeSentimentBatch(request);
					handleSentimentSuccess(job, response);
				}
				case REVIEW_SUMMARY -> {
					AiSummaryBatchRequest request = AiSummaryBatchRequest.singleRestaurant(restaurantId);
					AiSummaryBatchResponse response = aiClient.summarizeBatch(request);
					handleSummarySuccess(job, response);
				}
				case VECTOR_UPLOAD, RESTAURANT_COMPARISON -> {
					log.warn("Unsupported job type for review analysis: jobId={}, type={}", job.getId(),
						job.getJobType());
					markFailedInTx(job);
				}
			}
		} catch (AiServerException e) {
			log.error("Review analysis AI call failed: jobId={}, restaurantId={}, type={}",
				job.getId(), restaurantId, job.getJobType(), e);
			markFailedInTx(job);
		}
	}

	private void handleSentimentSuccess(AiJob job, AiSentimentBatchResponse response) {
		if (response.results() == null || response.results().isEmpty()) {
			log.warn("Sentiment batch returned no results: jobId={}", job.getId());
			markFailedInTx(job);
			return;
		}
		AiSentimentAnalysisResponse first = response.results().get(0);
		long vectorEpoch = job.getBaseEpoch();
		Instant analyzedAt = Instant.now();
		RestaurantReviewSentiment entity = RestaurantReviewSentiment.create(
			job.getRestaurantId(), vectorEpoch, null,
			first.positiveCount(), first.negativeCount(), first.neutralCount(),
			first.positiveRatio(), first.negativeRatio(), first.neutralRatio(),
			analyzedAt);
		transactionTemplate.executeWithoutResult(__ -> {
			sentimentRepository.save(entity);
			job.markCompleted();
			aiJobRepository.save(job);
		});
	}

	private void handleSummarySuccess(AiJob job, AiSummaryBatchResponse response) {
		if (response.results() == null || response.results().isEmpty()) {
			log.warn("Summary batch returned no results: jobId={}", job.getId());
			markFailedInTx(job);
			return;
		}
		AiSummaryDisplayResponse first = response.results().get(0);
		Map<String, Object> summaryJson = new HashMap<>();
		summaryJson.put("overall_summary", first.overallSummary());
		summaryJson.put("categories", first.categories() != null ? first.categories() : Collections.emptyMap());
		RestaurantReviewSummary entity = RestaurantReviewSummary.create(
			job.getRestaurantId(), job.getBaseEpoch(), null, summaryJson, Instant.now());
		transactionTemplate.executeWithoutResult(__ -> {
			summaryRepository.save(entity);
			job.markCompleted();
			aiJobRepository.save(job);
		});
	}

	private void markFailedInTx(AiJob job) {
		transactionTemplate.executeWithoutResult(__ -> {
			job.markFailed();
			aiJobRepository.save(job);
		});
	}
}
