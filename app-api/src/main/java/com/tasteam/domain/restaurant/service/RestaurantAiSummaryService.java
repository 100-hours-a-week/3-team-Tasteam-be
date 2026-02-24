package com.tasteam.domain.restaurant.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.entity.RestaurantComparison;
import com.tasteam.domain.restaurant.repository.RestaurantComparisonRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSentimentRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSummaryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantAiSummaryService {

	private static final String REVIEW_SUMMARY_FALLBACK = "리뷰가 더 모이면 AI 요약이 제공됩니다.";

	private final RestaurantComparisonRepository restaurantComparisonRepository;
	private final RestaurantReviewSummaryRepository restaurantReviewSummaryRepository;
	private final RestaurantReviewSentimentRepository restaurantReviewSentimentRepository;

	@Transactional(readOnly = true)
	public RestaurantAiSummaryResult getRestaurantAiSummary(long restaurantId) {
		String aiSummary = restaurantReviewSummaryRepository.findByRestaurantId(restaurantId)
			.map(s -> toSummaryString(s.getSummaryJson()))
			.orElse(null);
		Long positiveRatio = restaurantReviewSentimentRepository.findByRestaurantId(restaurantId)
			.map(s -> (long)s.getPositivePercent())
			.orElse(null);

		String aiFeature = restaurantComparisonRepository.findByRestaurantId(restaurantId)
			.map(RestaurantComparison::getComparisonJson)
			.map(m -> m.get("comparison_display"))
			.filter(o -> o instanceof List<?>)
			.map(o -> (List<?>)o)
			.filter(l -> !l.isEmpty())
			.map(l -> l.get(0).toString())
			.orElse(null);

		return new RestaurantAiSummaryResult(aiSummary, aiFeature, positiveRatio);
	}

	@Transactional(readOnly = true)
	public Map<Long, String> getReviewSummariesWithFallback(List<Long> restaurantIds) {
		Map<Long, String> summaries = restaurantReviewSummaryRepository.findByRestaurantIdIn(restaurantIds)
			.stream()
			.collect(Collectors.toMap(
				s -> s.getRestaurantId(),
				s -> toSummaryString(s.getSummaryJson())));

		return restaurantIds.stream()
			.collect(Collectors.toMap(
				restaurantId -> restaurantId,
				restaurantId -> summaries.getOrDefault(restaurantId, REVIEW_SUMMARY_FALLBACK)));
	}

	private static String toSummaryString(Map<String, Object> summaryJson) {
		if (summaryJson == null) {
			return null;
		}
		Object overall = summaryJson.get("overall_summary");
		return overall != null ? overall.toString() : null;
	}

	public record RestaurantAiSummaryResult(
		String summary,
		String feature,
		Long positiveRatio) {
	}
}
