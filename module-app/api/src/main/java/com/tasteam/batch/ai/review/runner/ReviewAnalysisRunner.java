package com.tasteam.batch.ai.review.runner;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tasteam.domain.restaurant.service.analysis.RestaurantReviewAnalysisSnapshotService;
import com.tasteam.global.metrics.MetricLabelPolicy;
import com.tasteam.infra.ai.AiClient;
import com.tasteam.infra.ai.dto.AiSentimentBatchRequest;
import com.tasteam.infra.ai.dto.AiSentimentBatchResponse;
import com.tasteam.infra.ai.dto.AiStrengthsRequest;
import com.tasteam.infra.ai.dto.AiStrengthsResponse;
import com.tasteam.infra.ai.dto.AiSummaryBatchRequest;
import com.tasteam.infra.ai.dto.AiSummaryBatchResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
	private final MeterRegistry meterRegistry;

	/**
	 * 감정 분석 실행. 성공 시 Snapshot 저장 후 Success, 결과 없거나 예외 시 Failure.
	 */
	public AnalysisRunResult runSentimentAnalysis(long restaurantId, long vectorEpoch) {
		Timer.Sample totalSample = Timer.start(meterRegistry);
		String result = "failed";
		try {
			Timer.Sample invokeSample = Timer.start(meterRegistry);
			AiSentimentBatchResponse response = aiClient.analyzeSentimentBatch(
				AiSentimentBatchRequest.singleRestaurant(restaurantId));
			recordTimer("ai.review_analysis.ai_invoke.duration", invokeSample, "type", "sentiment", "result",
				"success");
			if (response.results() == null || response.results().isEmpty()) {
				log.warn("Sentiment batch returned no results: restaurantId={}", restaurantId);
				result = "empty";
				return new AnalysisRunResult.Failure(new IllegalStateException("No sentiment results"));
			}
			Instant analyzedAt = Instant.now();
			Timer.Sample saveSample = Timer.start(meterRegistry);
			snapshotService.saveSentiment(restaurantId, vectorEpoch, response.results().get(0), analyzedAt);
			recordTimer("ai.review_analysis.snapshot_save.duration", saveSample, "type", "sentiment", "result",
				"success");
			result = "success";
			return new AnalysisRunResult.Success();
		} catch (Exception e) {
			recordCounter("ai.review_analysis.execute.total", "type", "sentiment", "result", "failed");
			recordTimer("ai.review_analysis.execute.duration", totalSample, "type", "sentiment", "result", "failed");
			return new AnalysisRunResult.Failure(e);
		} finally {
			if (!"failed".equals(result)) {
				recordCounter("ai.review_analysis.execute.total", "type", "sentiment", "result", result);
				recordTimer("ai.review_analysis.execute.duration", totalSample, "type", "sentiment", "result", result);
			}
		}
	}

	/**
	 * 요약 분석 실행. 성공 시 Snapshot 저장 후 Success, 결과 없거나 예외 시 Failure.
	 */
	public AnalysisRunResult runSummaryAnalysis(long restaurantId, long vectorEpoch) {
		Timer.Sample totalSample = Timer.start(meterRegistry);
		String result = "failed";
		try {
			Timer.Sample invokeSample = Timer.start(meterRegistry);
			AiSummaryBatchResponse response = aiClient.summarizeBatch(
				AiSummaryBatchRequest.singleRestaurant(restaurantId));
			recordTimer("ai.review_analysis.ai_invoke.duration", invokeSample, "type", "summary", "result",
				"success");
			if (response.results() == null || response.results().isEmpty()) {
				log.warn("Summary batch returned no results: restaurantId={}", restaurantId);
				result = "empty";
				return new AnalysisRunResult.Failure(new IllegalStateException("No summary results"));
			}
			Instant analyzedAt = Instant.now();
			Timer.Sample saveSample = Timer.start(meterRegistry);
			snapshotService.saveSummary(restaurantId, vectorEpoch, response.results().get(0), analyzedAt);
			recordTimer("ai.review_analysis.snapshot_save.duration", saveSample, "type", "summary", "result",
				"success");
			result = "success";
			return new AnalysisRunResult.Success();
		} catch (Exception e) {
			recordCounter("ai.review_analysis.execute.total", "type", "summary", "result", "failed");
			recordTimer("ai.review_analysis.execute.duration", totalSample, "type", "summary", "result", "failed");
			return new AnalysisRunResult.Failure(e);
		} finally {
			if (!"failed".equals(result)) {
				recordCounter("ai.review_analysis.execute.total", "type", "summary", "result", result);
				recordTimer("ai.review_analysis.execute.duration", totalSample, "type", "summary", "result", result);
			}
		}
	}

	/**
	 * 비교 분석 실행. 성공 시 Snapshot 저장 후 Success, 예외 시 Failure.
	 */
	public AnalysisRunResult runComparisonAnalysis(long restaurantId) {
		Timer.Sample totalSample = Timer.start(meterRegistry);
		String result = "failed";
		try {
			Timer.Sample invokeSample = Timer.start(meterRegistry);
			AiStrengthsResponse response = aiClient.extractStrengths(
				new AiStrengthsRequest(restaurantId, COMPARISON_TOP_K));
			recordTimer("ai.review_analysis.ai_invoke.duration", invokeSample, "type", "comparison", "result",
				"success");
			Map<String, BigDecimal> categoryLift = toBigDecimalMap(response.categoryLift());
			List<String> comparisonDisplay = Optional.ofNullable(response.strengthDisplay()).orElse(List.of());
			Instant analyzedAt = Instant.now();
			Timer.Sample saveSample = Timer.start(meterRegistry);
			snapshotService.saveComparison(
				restaurantId,
				categoryLift,
				comparisonDisplay,
				response.totalCandidates(),
				response.validatedCount(),
				analyzedAt);
			recordTimer("ai.review_analysis.snapshot_save.duration", saveSample, "type", "comparison", "result",
				"success");
			result = "success";
			return new AnalysisRunResult.Success();
		} catch (Exception e) {
			recordCounter("ai.review_analysis.execute.total", "type", "comparison", "result", "failed");
			recordTimer("ai.review_analysis.execute.duration", totalSample, "type", "comparison", "result", "failed");
			return new AnalysisRunResult.Failure(e);
		} finally {
			if (!"failed".equals(result)) {
				recordCounter("ai.review_analysis.execute.total", "type", "comparison", "result", result);
				recordTimer("ai.review_analysis.execute.duration", totalSample, "type", "comparison", "result", result);
			}
		}
	}

	private void recordCounter(String metricName, String... tags) {
		MetricLabelPolicy.validate(metricName, tags);
		meterRegistry.counter(metricName, tags).increment();
	}

	private void recordTimer(String metricName, Timer.Sample sample, String... tags) {
		MetricLabelPolicy.validate(metricName, tags);
		sample.stop(Timer.builder(metricName).tags(tags).register(meterRegistry));
	}

	private static Map<String, BigDecimal> toBigDecimalMap(Map<String, Double> source) {
		if (source == null || source.isEmpty()) {
			return Map.of();
		}
		return source.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> BigDecimal.valueOf(e.getValue())));
	}
}
