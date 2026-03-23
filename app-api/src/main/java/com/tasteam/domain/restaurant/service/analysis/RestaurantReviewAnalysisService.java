package com.tasteam.domain.restaurant.service.analysis;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.infra.ai.AiClient;
import com.tasteam.infra.ai.dto.AiSentimentAnalysisResponse;
import com.tasteam.infra.ai.dto.AiSentimentRequest;
import com.tasteam.infra.ai.dto.AiStrengthsRequest;
import com.tasteam.infra.ai.dto.AiStrengthsResponse;
import com.tasteam.infra.ai.dto.AiSummaryDisplayResponse;
import com.tasteam.infra.ai.dto.AiSummaryRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantReviewAnalysisService {

	private static final double DEFAULT_SUMMARY_MIN_SCORE = 0.0d;

	private final AnalysisLock analysisLock;
	private final SummarySentimentAnalysisTriggerPolicy summarySentimentTriggerPolicy;
	private final ComparisonAnalysisTriggerPolicy comparisonTriggerPolicy;
	private final RestaurantReviewAnalysisPolicyProperties policyProperties;
	private final RestaurantReviewAnalysisStateService stateService;
	private final RestaurantReviewAnalysisSnapshotService snapshotService;
	private final AiClient aiClient;
	private final ReviewRepository reviewRepository;

	public void onReviewCreated(long restaurantId) {
		if (!analysisLock.tryLock(restaurantId)) {
			log.debug("AI analysis skipped due to lock contention. restaurantId={}", restaurantId);
			return;
		}

		try {
			int reviewCount = Math.toIntExact(reviewRepository.countByRestaurantIdAndDeletedAtIsNull(restaurantId));
			log.debug("AI analysis trigger check on review-created. restaurantId={}, reviewCount={}", restaurantId,
				reviewCount);

			if (summarySentimentTriggerPolicy.shouldRun(reviewCount)) {
				executeSummaryAndSentiment(restaurantId);
			} else {
				log.debug("Summary/Sentiment analysis skipped by policy. restaurantId={}, reviewCount={}",
					restaurantId, reviewCount);
			}
		} catch (Exception e) {
			log.warn("AI review-created analysis failed. restaurantId={}", restaurantId, e);

		} finally {
			analysisLock.unlock(restaurantId);
		}
	}

	private void executeSummaryAndSentiment(long restaurantId) {
		log.info("Summary/Sentiment analysis started. restaurantId={}", restaurantId);
		stateService.markAnalyzingForSummary(restaurantId);
		try {
			int batchSize = policyProperties.getSummaryBatchSize();
			List<Review> reviews = reviewRepository
				.findByRestaurantIdAndDeletedAtIsNull(
					restaurantId,
					PageRequest.of(
						0,
						batchSize,
						Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
			List<AiSentimentRequest.ReviewContent> reviewContents = reviews.stream()
				.map(review -> new AiSentimentRequest.ReviewContent(
					Math.toIntExact(review.getId()),
					restaurantId,
					review.getContent() == null ? "" : review.getContent(),
					review.getCreatedAt() == null ? Instant.now() : review.getCreatedAt()))
				.toList();

			AiSummaryDisplayResponse summary = aiClient
				.summarize(new AiSummaryRequest(
					restaurantId,
					batchSize,
					DEFAULT_SUMMARY_MIN_SCORE));
			AiSentimentAnalysisResponse sentiment = aiClient
				.analyzeSentimentDebug(new AiSentimentRequest(restaurantId, reviewContents));

			snapshotService.saveSummary(
				restaurantId,
				0L,
				summary,
				Instant.now());
			snapshotService.saveSentiment(
				restaurantId,
				0L,
				sentiment,
				Instant.now());
			log.info("Summary/Sentiment analysis completed. restaurantId={}", restaurantId);

		} catch (Exception e) {
			log.warn("Summary/Sentiment AI analysis failed. restaurantId={}", restaurantId, e);
			stateService.markCompletedForSummary(restaurantId);
		}
	}

	private void executeComparison(long restaurantId) {
		log.info("Comparison analysis started. restaurantId={}", restaurantId);
		stateService.markAnalyzingForComparison(restaurantId);
		try {
			AiStrengthsResponse response = aiClient.extractStrengths(
				new AiStrengthsRequest(restaurantId, Math.toIntExact(policyProperties.getComparisonBatchSize())));
			Map<String, BigDecimal> categoryLift = toBigDecimalMap(response.categoryLift());
			if (categoryLift.isEmpty()) {
				categoryLift = toBigDecimalMapFromStrengths(response.strengths());
			}
			List<String> comparisonDisplay = Optional.ofNullable(response.strengthDisplay()).orElse(List.of());
			Instant analyzedAt = Instant.now();
			log.debug(
				"Comparison response received. restaurantId={}, categoryLiftSize={}, strengthsSize={}, displaySize={}, totalCandidates={}, validatedCount={}",
				restaurantId,
				categoryLift.size(),
				response.strengths() == null ? 0 : response.strengths().size(),
				comparisonDisplay.size(),
				response.totalCandidates(),
				response.validatedCount());

			snapshotService.saveComparison(
				restaurantId,
				categoryLift,
				comparisonDisplay,
				response.totalCandidates(),
				response.validatedCount(),
				analyzedAt);
			log.info("Comparison analysis completed. restaurantId={}", restaurantId);
		} catch (Exception e) {
			log.warn("Comparison AI analysis failed. restaurantId={}", restaurantId, e);
			stateService.markCompletedForComparison(restaurantId);
		}
	}

	private Map<String, String> extractCategorySummaries(AiSummaryDisplayResponse summary) {
		if (summary.categories() == null) {
			return Map.of();
		}
		return summary.categories().entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue() == null || entry.getValue().summary() == null ? ""
					: entry.getValue().summary()));
	}

	private Map<String, BigDecimal> toBigDecimalMap(Map<String, Double> source) {
		if (source == null || source.isEmpty()) {
			return Map.of();
		}
		return source.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> BigDecimal.valueOf(entry.getValue())));
	}

	private Map<String, BigDecimal> toBigDecimalMapFromStrengths(List<AiStrengthsResponse.StrengthItem> strengths) {
		if (strengths == null || strengths.isEmpty()) {
			return Map.of();
		}
		return strengths.stream()
			.filter(item -> item != null && item.category() != null)
			.collect(Collectors.toMap(
				AiStrengthsResponse.StrengthItem::category,
				item -> BigDecimal.valueOf(item.liftPercentage()),
				(first, second) -> second));
	}
}
