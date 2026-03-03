package com.tasteam.batch.ai.review.runner;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tasteam.domain.restaurant.service.analysis.RestaurantReviewAnalysisSnapshotService;
import com.tasteam.infra.ai.AiClient;
import com.tasteam.infra.ai.dto.AiSentimentBatchRequest;
import com.tasteam.infra.ai.dto.AiSentimentBatchResponse;
import com.tasteam.infra.ai.dto.AiStrengthsRequest;
import com.tasteam.infra.ai.dto.AiStrengthsResponse;
import com.tasteam.infra.ai.dto.AiSummaryBatchRequest;
import com.tasteam.infra.ai.dto.AiSummaryBatchResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 감정·요약·비교 분석 실행 (AI 호출 + Snapshot 저장).
 * 배치 워커 및 이벤트 핸들러에서 공통 사용. toBigDecimalMap은 Runner에만 유지.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewAnalysisRunner {

	private static final int COMPARISON_TOP_K = 10;

	private final AiClient aiClient;
	private final RestaurantReviewAnalysisSnapshotService snapshotService;

	/**
	 * 감정 분석 실행. 성공 시 Snapshot 저장 후 Success, 결과 없거나 예외 시 Failure.
	 */
	public AnalysisRunResult runSentimentAnalysis(long restaurantId, long vectorEpoch) {
		try {
			AiSentimentBatchResponse response = aiClient.analyzeSentimentBatch(
				AiSentimentBatchRequest.singleRestaurant(restaurantId));
			if (response.results() == null || response.results().isEmpty()) {
				log.warn("Sentiment batch returned no results: restaurantId={}", restaurantId);
				return new AnalysisRunResult.Failure(new IllegalStateException("No sentiment results"));
			}
			Instant analyzedAt = Instant.now();
			snapshotService.saveSentiment(restaurantId, vectorEpoch, response.results().get(0), analyzedAt);
			return new AnalysisRunResult.Success();
		} catch (Exception e) {
			return new AnalysisRunResult.Failure(e);
		}
	}

	/**
	 * 요약 분석 실행. 성공 시 Snapshot 저장 후 Success, 결과 없거나 예외 시 Failure.
	 */
	public AnalysisRunResult runSummaryAnalysis(long restaurantId, long vectorEpoch) {
		try {
			AiSummaryBatchResponse response = aiClient.summarizeBatch(
				AiSummaryBatchRequest.singleRestaurant(restaurantId));
			if (response.results() == null || response.results().isEmpty()) {
				log.warn("Summary batch returned no results: restaurantId={}", restaurantId);
				return new AnalysisRunResult.Failure(new IllegalStateException("No summary results"));
			}
			Instant analyzedAt = Instant.now();
			snapshotService.saveSummary(restaurantId, vectorEpoch, response.results().get(0), analyzedAt);
			return new AnalysisRunResult.Success();
		} catch (Exception e) {
			return new AnalysisRunResult.Failure(e);
		}
	}

	/**
	 * 비교 분석 실행. 성공 시 Snapshot 저장 후 Success, 예외 시 Failure.
	 */
	public AnalysisRunResult runComparisonAnalysis(long restaurantId) {
		try {
			AiStrengthsResponse response = aiClient.extractStrengths(
				new AiStrengthsRequest(restaurantId, COMPARISON_TOP_K));
			Map<String, BigDecimal> categoryLift = toBigDecimalMap(response.categoryLift());
			List<String> comparisonDisplay = Optional.ofNullable(response.strengthDisplay()).orElse(List.of());
			Instant analyzedAt = Instant.now();
			snapshotService.saveComparison(
				restaurantId,
				categoryLift,
				comparisonDisplay,
				response.totalCandidates(),
				response.validatedCount(),
				analyzedAt);
			return new AnalysisRunResult.Success();
		} catch (Exception e) {
			return new AnalysisRunResult.Failure(e);
		}
	}

	private static Map<String, BigDecimal> toBigDecimalMap(Map<String, Double> source) {
		if (source == null || source.isEmpty()) {
			return Map.of();
		}
		return source.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> BigDecimal.valueOf(e.getValue())));
	}
}
