package com.tasteam.domain.restaurant.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.restaurant.dto.RestaurantAiCategoryComparison;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantAiSummaryService {

	private static final String REVIEW_SUMMARY_FALLBACK = "리뷰가 더 모이면 AI 요약이 제공됩니다.";
	private static final int MAX_EVIDENCES = 3;

	private final RestaurantComparisonRepository restaurantComparisonRepository;
	private final RestaurantReviewSummaryRepository restaurantReviewSummaryRepository;
	private final RestaurantReviewSentimentRepository restaurantReviewSentimentRepository;

	@Transactional(readOnly = true)
	public RestaurantAiDetails getRestaurantAiDetails(long restaurantId) {
		RestaurantAiSentiment sentiment = restaurantReviewSentimentRepository.findByRestaurantId(restaurantId)
			.map(s -> new RestaurantAiSentiment(
				s.getPositivePercent() != null ? s.getPositivePercent().intValue() : null))
			.orElse(null);
		RestaurantAiSummary summary = restaurantReviewSummaryRepository.findByRestaurantId(restaurantId)
			.map(s -> toRestaurantAiSummary(s.getSummaryJson()))
			.orElse(null);
		RestaurantAiComparison comparison = restaurantComparisonRepository.findByRestaurantId(restaurantId)
			.map(c -> toRestaurantAiComparison(c.getComparisonJson()))
			.orElse(null);
		return new RestaurantAiDetails(sentiment, summary, comparison);
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

	private RestaurantAiSummary toRestaurantAiSummary(Map<String, Object> summaryJson) {
		if (summaryJson == null) {
			return null;
		}
		String overallSummary = summaryJson.get("overall_summary") != null
			? summaryJson.get("overall_summary").toString()
			: null;
		@SuppressWarnings("unchecked") Map<String, Object> categoriesRaw = (Map<String, Object>)summaryJson
			.get("categories");
		Map<AiReviewCategory, RestaurantAiCategorySummary> categoryDetails = parseCategorySummaries(categoriesRaw);
		return new RestaurantAiSummary(overallSummary, categoryDetails != null ? categoryDetails : Map.of());
	}

	private Map<AiReviewCategory, RestaurantAiCategorySummary> parseCategorySummaries(
		Map<String, Object> categoriesRaw) {
		if (categoriesRaw == null || categoriesRaw.isEmpty()) {
			return Map.of();
		}
		Map<AiReviewCategory, RestaurantAiCategorySummary> result = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : categoriesRaw.entrySet()) {
			AiReviewCategory.fromJsonKey(entry.getKey()).ifPresent(category -> {
				RestaurantAiCategorySummary detail = parseCategorySummary(entry.getValue());
				if (detail != null) {
					result.put(category, detail);
				}
			});
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private RestaurantAiCategorySummary parseCategorySummary(Object value) {
		if (!(value instanceof Map<?, ?> map)) {
			return null;
		}
		Map<String, Object> m = (Map<String, Object>)map;
		String summary = m.get("summary") != null ? m.get("summary").toString() : null;
		List<String> bullets = parseBullets(m.get("bullets"));
		List<RestaurantAiEvidence> evidences = parseEvidences(m.get("evidence"));
		return new RestaurantAiCategorySummary(summary, bullets, evidences);
	}

	private List<String> parseBullets(Object value) {
		if (!(value instanceof List<?> list)) {
			return List.of();
		}
		return list.stream()
			.map(o -> o != null ? o.toString() : null)
			.filter(s -> s != null)
			.toList();
	}

	@SuppressWarnings("unchecked")
	private List<RestaurantAiEvidence> parseEvidences(Object value) {
		if (!(value instanceof List<?> list)) {
			return List.of();
		}
		return list.stream()
			.limit(MAX_EVIDENCES)
			.filter(item -> item instanceof Map<?, ?>)
			.map(item -> (Map<String, Object>)item)
			.map(e -> new RestaurantAiEvidence(
				parseReviewId(e.get("review_id")),
				e.get("snippet") != null ? e.get("snippet").toString() : null))
			.toList();
	}

	private Long parseReviewId(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private RestaurantAiComparison toRestaurantAiComparison(Map<String, Object> comparisonJson) {
		if (comparisonJson == null) {
			return null;
		}
		String overallComparison = null;
		Object displayObj = comparisonJson.get("comparison_display");
		if (displayObj instanceof List<?> list && !list.isEmpty()) {
			overallComparison = list.get(0).toString();
		}
		Map<AiReviewCategory, RestaurantAiCategoryComparison> categoryDetails = parseCategoryComparisons(
			comparisonJson.get("category_lift"));
		return new RestaurantAiComparison(overallComparison, categoryDetails != null ? categoryDetails : Map.of());
	}

	@SuppressWarnings("unchecked")
	private Map<AiReviewCategory, RestaurantAiCategoryComparison> parseCategoryComparisons(Object categoryLiftObj) {
		if (!(categoryLiftObj instanceof Map<?, ?> map)) {
			return Map.of();
		}
		Map<String, Object> m = (Map<String, Object>)map;
		Map<AiReviewCategory, RestaurantAiCategoryComparison> result = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : m.entrySet()) {
			AiReviewCategory.fromJsonKey(entry.getKey()).ifPresent(category -> {
				Double liftScore = toDouble(entry.getValue());
				result.put(category, new RestaurantAiCategoryComparison(
					null, List.of(), List.of(), liftScore));
			});
		}
		return result;
	}

	private Double toDouble(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number n) {
			return n.doubleValue();
		}
		try {
			return Double.parseDouble(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String toSummaryString(Map<String, Object> summaryJson) {
		if (summaryJson == null) {
			return null;
		}
		Object overall = summaryJson.get("overall_summary");
		return overall != null ? overall.toString() : null;
	}
}
