package com.tasteam.domain.restaurant.service.analysis;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.entity.RestaurantComparison;
import com.tasteam.domain.restaurant.entity.RestaurantReviewSentiment;
import com.tasteam.domain.restaurant.entity.RestaurantReviewSummary;
import com.tasteam.domain.restaurant.repository.RestaurantComparisonRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSentimentRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSummaryRepository;
import com.tasteam.infra.ai.dto.AiSentimentAnalysisResponse;
import com.tasteam.infra.ai.dto.AiSummaryDisplayResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantReviewAnalysisSnapshotService {

	private final RestaurantComparisonRepository restaurantComparisonRepository;
	private final RestaurantReviewSentimentRepository sentimentRepository;
	private final RestaurantReviewSummaryRepository summaryRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveComparison(
		long restaurantId,
		Map<String, BigDecimal> categoryLift,
		List<String> comparisonDisplay,
		int totalCandidates,
		int validatedCount,
		Instant analyzedAt) {
		Map<String, Object> comparisonJson = new HashMap<>();
		comparisonJson.put("category_lift",
			categoryLift != null ? categoryLift.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().doubleValue())) : Map.of());
		comparisonJson.put("comparison_display", comparisonDisplay != null ? comparisonDisplay : List.of());
		comparisonJson.put("total_candidates", totalCandidates);
		comparisonJson.put("validated_count", validatedCount);

		RestaurantComparison snapshot = restaurantComparisonRepository.findByRestaurantId(restaurantId)
			.orElse(null);
		if (snapshot != null) {
			snapshot.update(comparisonJson, analyzedAt);
		} else {
			snapshot = RestaurantComparison.create(restaurantId, null, comparisonJson, analyzedAt);
		}
		restaurantComparisonRepository.save(snapshot);
	}

	/**
	 * 감정 분석 결과를 저장(restaurant_id당 1건, 갱신 방식). 최초 분석·배치 Job 공통 사용.
	 */
	@Transactional
	public void saveSentiment(
		long restaurantId,
		long vectorEpoch,
		AiSentimentAnalysisResponse result,
		Instant analyzedAt) {
		RestaurantReviewSentiment sentiment = sentimentRepository.findByRestaurantId(restaurantId).orElse(null);
		if (sentiment != null) {
			sentiment.update(
				vectorEpoch,
				result.positiveCount(), result.negativeCount(), result.neutralCount(),
				result.positiveRatio(), result.negativeRatio(), result.neutralRatio(),
				analyzedAt);
		} else {
			sentiment = RestaurantReviewSentiment.create(
				restaurantId, vectorEpoch, null,
				result.positiveCount(), result.negativeCount(), result.neutralCount(),
				result.positiveRatio(), result.negativeRatio(), result.neutralRatio(),
				analyzedAt);
		}
		sentimentRepository.save(sentiment);
	}

	/**
	 * 요약 분석 결과를 저장(restaurant_id당 1건, 갱신 방식). 최초 분석·배치 Job 공통 사용.
	 */
	@Transactional
	public void saveSummary(
		long restaurantId,
		long vectorEpoch,
		AiSummaryDisplayResponse result,
		Instant analyzedAt) {
		Map<String, Object> summaryJson = new HashMap<>();
		summaryJson.put("overall_summary", result.overallSummary());
		summaryJson.put("categories", result.categories() != null ? result.categories() : Collections.emptyMap());

		RestaurantReviewSummary summary = summaryRepository.findByRestaurantId(restaurantId).orElse(null);
		if (summary != null) {
			summary.update(summaryJson, vectorEpoch, analyzedAt);
		} else {
			summary = RestaurantReviewSummary.create(restaurantId, vectorEpoch, null, summaryJson, analyzedAt);
		}
		summaryRepository.save(summary);
	}
}
