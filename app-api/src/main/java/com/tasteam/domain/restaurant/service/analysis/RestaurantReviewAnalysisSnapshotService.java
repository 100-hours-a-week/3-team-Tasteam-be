package com.tasteam.domain.restaurant.service.analysis;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.entity.AiRestaurantComparison;
import com.tasteam.domain.restaurant.entity.AiRestaurantReviewAnalysis;
import com.tasteam.domain.restaurant.repository.AiRestaurantComparisonRepository;
import com.tasteam.domain.restaurant.repository.AiRestaurantReviewAnalysisRepository;
import com.tasteam.domain.restaurant.type.AnalysisStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantReviewAnalysisSnapshotService {

	private final AiRestaurantReviewAnalysisRepository aiRestaurantReviewAnalysisRepository;
	private final AiRestaurantComparisonRepository aiRestaurantComparisonRepository;

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
		AiRestaurantComparison snapshot = aiRestaurantComparisonRepository.findByRestaurantId(restaurantId)
			.orElseGet(() -> AiRestaurantComparison.createEmpty(restaurantId, AnalysisStatus.ANALYZING));

		snapshot.updateComparison(categoryLift, comparisonDisplay, totalCandidates, validatedCount, analyzedAt);
		aiRestaurantComparisonRepository.save(snapshot);
	}
}
