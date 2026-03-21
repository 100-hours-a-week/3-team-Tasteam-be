package com.tasteam.domain.restaurant.service;

import static com.tasteam.domain.restaurant.service.RestaurantAiJsonParser.extractSummaryTextOrDefault;
import static com.tasteam.domain.restaurant.service.RestaurantAiJsonParser.parseComparison;
import static com.tasteam.domain.restaurant.service.RestaurantAiJsonParser.parseSummary;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.dto.RestaurantAiCategorySummary;
import com.tasteam.domain.restaurant.dto.RestaurantAiComparison;
import com.tasteam.domain.restaurant.dto.RestaurantAiDetails;
import com.tasteam.domain.restaurant.dto.RestaurantAiEvidence;
import com.tasteam.domain.restaurant.dto.RestaurantAiSentiment;
import com.tasteam.domain.restaurant.dto.RestaurantAiSummary;
import com.tasteam.domain.restaurant.repository.RestaurantComparisonRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSentimentRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSummaryRepository;
import com.tasteam.domain.restaurant.type.AiReviewCategory;
import com.tasteam.domain.review.entity.Review;
import com.tasteam.domain.review.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantAiSummaryService {

	private static final String REVIEW_SUMMARY_FALLBACK = "리뷰가 더 모이면 AI 요약이 제공됩니다.";

	private final RestaurantComparisonRepository restaurantComparisonRepository;
	private final RestaurantReviewSummaryRepository restaurantReviewSummaryRepository;
	private final RestaurantReviewSentimentRepository restaurantReviewSentimentRepository;
	private final ReviewRepository reviewRepository;

	@Transactional(readOnly = true)
	public RestaurantAiDetails getRestaurantAiDetails(long restaurantId) {
		RestaurantAiSentiment sentiment = restaurantReviewSentimentRepository.findByRestaurantId(restaurantId)
			.map(s -> new RestaurantAiSentiment(
				s.getPositivePercent() != null ? s.getPositivePercent().intValue() : null))
			.orElse(null);
		RestaurantAiSummary summary = restaurantReviewSummaryRepository.findByRestaurantId(restaurantId)
			.map(s -> parseSummary(s.getSummaryJson()))
			.map(this::enrichSummary)
			.orElse(null);
		RestaurantAiComparison comparison = restaurantComparisonRepository.findByRestaurantId(restaurantId)
			.map(c -> parseComparison(c.getComparisonJson()))
			.orElse(null);
		return new RestaurantAiDetails(sentiment, summary, comparison);
	}

	@Transactional(readOnly = true)
	public Map<Long, String> getReviewSummariesWithFallback(List<Long> restaurantIds) {
		Map<Long, String> summaries = restaurantReviewSummaryRepository.findByRestaurantIdIn(restaurantIds)
			.stream()
			.collect(Collectors.toMap(
				s -> s.getRestaurantId(),
				s -> extractSummaryTextOrDefault(s.getSummaryJson(), REVIEW_SUMMARY_FALLBACK),
				(existing, replacement) -> existing,
				LinkedHashMap::new));

		return restaurantIds.stream()
			.collect(Collectors.toMap(
				restaurantId -> restaurantId,
				restaurantId -> summaries.getOrDefault(restaurantId, REVIEW_SUMMARY_FALLBACK),
				(existing, replacement) -> existing,
				LinkedHashMap::new));
	}

	private RestaurantAiSummary enrichSummary(RestaurantAiSummary summary) {
		if (summary == null || summary.categoryDetails() == null || summary.categoryDetails().isEmpty()) {
			return summary;
		}

		Map<AiReviewCategory, RestaurantAiCategorySummary> categoryDetails = summary.categoryDetails();
		Set<Long> reviewIds = categoryDetails.values().stream()
			.flatMap(detail -> detail.evidences() != null ? detail.evidences().stream() : Stream.of())
			.map(RestaurantAiEvidence::reviewId)
			.filter(id -> id != null)
			.collect(Collectors.toSet());
		if (reviewIds.isEmpty()) {
			return summary;
		}

		Map<Long, Review> reviewMap = reviewRepository.findByIdInAndDeletedAtIsNull(reviewIds).stream()
			.collect(Collectors.toMap(Review::getId, review -> review));
		Map<AiReviewCategory, RestaurantAiCategorySummary> enriched = categoryDetails.entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> enrichCategorySummary(entry.getValue(), reviewMap),
				(existing, replacement) -> existing,
				LinkedHashMap::new));
		return new RestaurantAiSummary(summary.overallSummary(), enriched);
	}

	private RestaurantAiCategorySummary enrichCategorySummary(
		RestaurantAiCategorySummary summary,
		Map<Long, Review> reviewMap) {
		if (summary.evidences() == null || summary.evidences().isEmpty()) {
			return summary;
		}
		List<RestaurantAiEvidence> enriched = summary.evidences().stream()
			.map(ev -> enrichEvidence(ev, reviewMap.get(ev.reviewId())))
			.toList();
		return new RestaurantAiCategorySummary(summary.summary(), summary.bullets(), enriched);
	}

	private RestaurantAiEvidence enrichEvidence(RestaurantAiEvidence ev, Review review) {
		if (review == null) {
			return ev;
		}
		Long authorId = review.getMember() != null ? review.getMember().getId() : null;
		String authorName = review.getMember() != null ? review.getMember().getNickname() : null;
		Instant createdAt = review.getCreatedAt();
		return new RestaurantAiEvidence(ev.reviewId(), ev.snippet(), authorId, authorName, createdAt);
	}
}
