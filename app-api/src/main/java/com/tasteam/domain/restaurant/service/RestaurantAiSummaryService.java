package com.tasteam.domain.restaurant.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.entity.AiRestaurantReviewAnalysis;
import com.tasteam.domain.restaurant.entity.RestaurantComparison;
import com.tasteam.domain.restaurant.repository.AiRestaurantReviewAnalysisRepository;
import com.tasteam.domain.restaurant.repository.RestaurantComparisonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantAiSummaryService {

	private static final String REVIEW_SUMMARY_FALLBACK = "리뷰가 더 모이면 AI 요약이 제공됩니다.";

	private final RestaurantComparisonRepository restaurantComparisonRepository;
	private final AiRestaurantReviewAnalysisRepository aiRestaurantReviewAnalysisRepository;

	@Transactional(readOnly = true)
	public RestaurantAiSummaryResult getRestaurantAiSummary(long restaurantId) {
		Optional<AiRestaurantReviewAnalysis> aiAnalysis = aiRestaurantReviewAnalysisRepository
			.findByRestaurantId(restaurantId);

		String aiSummary = aiAnalysis.map(AiRestaurantReviewAnalysis::getOverallSummary).orElse(null);
		Long positiveRatio = aiAnalysis
			.map(AiRestaurantReviewAnalysis::getPositiveRatio)
			.map(this::toPercentage)
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
		Map<Long, String> summaries = aiRestaurantReviewAnalysisRepository.findByRestaurantIdIn(restaurantIds)
			.stream()
			.collect(Collectors.toMap(
				AiRestaurantReviewAnalysis::getRestaurantId,
				AiRestaurantReviewAnalysis::getOverallSummary));

		return restaurantIds.stream()
			.collect(Collectors.toMap(
				restaurantId -> restaurantId,
				restaurantId -> summaries.getOrDefault(restaurantId, REVIEW_SUMMARY_FALLBACK)));
	}

	private Long toPercentage(BigDecimal ratio) {
		return ratio.multiply(BigDecimal.valueOf(100))
			.setScale(0, RoundingMode.HALF_UP)
			.longValueExact();
	}

	public record RestaurantAiSummaryResult(
		String summary,
		String feature,
		Long positiveRatio) {
	}
}
