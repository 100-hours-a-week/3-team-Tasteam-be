package com.tasteam.domain.restaurant.service.analysis;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.entity.AiRestaurantReviewAnalysis;
import com.tasteam.domain.restaurant.entity.RestaurantComparison;
import com.tasteam.domain.restaurant.repository.AiRestaurantReviewAnalysisRepository;
import com.tasteam.domain.restaurant.repository.RestaurantComparisonRepository;
import com.tasteam.domain.restaurant.type.AnalysisStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantReviewAnalysisSnapshotService {

	private final AiRestaurantReviewAnalysisRepository aiRestaurantReviewAnalysisRepository;
	private final RestaurantComparisonRepository restaurantComparisonRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void saveSummaryAndSentiment(
		long restaurantId,
		String overallSummary,
		Map<String, String> categorySummaries,
		BigDecimal positiveRatio,
		BigDecimal negativeRatio,
		Instant analyzedAt) {
		AiRestaurantReviewAnalysis snapshot = aiRestaurantReviewAnalysisRepository.findByRestaurantId(restaurantId)
			.orElseGet(() -> AiRestaurantReviewAnalysis.createEmpty(restaurantId, AnalysisStatus.ANALYZING));

		snapshot.updateAnalysis(overallSummary, categorySummaries, positiveRatio, negativeRatio, analyzedAt);
		aiRestaurantReviewAnalysisRepository.save(snapshot);
	}

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
			snapshot.updateResult(comparisonJson, analyzedAt);
		} else {
			snapshot = RestaurantComparison.create(restaurantId, null, comparisonJson, analyzedAt);
		}
		restaurantComparisonRepository.save(snapshot);
	}
}
