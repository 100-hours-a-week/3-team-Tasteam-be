package com.tasteam.domain.restaurant.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.tasteam.domain.restaurant.dto.RestaurantAiCategoryComparison;
import com.tasteam.domain.restaurant.dto.RestaurantAiCategorySummary;
import com.tasteam.domain.restaurant.dto.RestaurantAiComparison;
import com.tasteam.domain.restaurant.dto.RestaurantAiEvidence;
import com.tasteam.domain.restaurant.dto.RestaurantAiSummary;
import com.tasteam.domain.restaurant.type.AiReviewCategory;

public final class RestaurantAiJsonParser {

	private static final int MAX_EVIDENCES = 3;
	private static final List<AiReviewCategory> COMPARISON_DISPLAY_ORDER = List.of(
		AiReviewCategory.SERVICE,
		AiReviewCategory.PRICE);

	private RestaurantAiJsonParser() {}

	public static String extractSummaryText(Map<String, Object> summaryJson) {
		if (summaryJson == null || summaryJson.isEmpty()) {
			return null;
		}

		String overall = toNonBlankString(summaryJson.get("overall_summary"));
		if (overall != null) {
			return overall;
		}

		return toNonBlankString(summaryJson.get("summary"));
	}

	public static String extractSummaryTextOrDefault(Map<String, Object> summaryJson, String defaultValue) {
		String summary = extractSummaryText(summaryJson);
		return summary != null ? summary : defaultValue;
	}

	public static RestaurantAiSummary parseSummary(Map<String, Object> summaryJson) {
		if (summaryJson == null) {
			return null;
		}

		Map<AiReviewCategory, RestaurantAiCategorySummary> categoryDetails = parseCategorySummaries(
			asStringObjectMap(summaryJson.get("categories")));
		return new RestaurantAiSummary(extractSummaryText(summaryJson), categoryDetails);
	}

	public static RestaurantAiComparison parseComparison(Map<String, Object> comparisonJson) {
		if (comparisonJson == null) {
			return null;
		}

		List<String> comparisonDisplay = parseStrings(
			firstPresent(comparisonJson, "comparison_display", "strength_display"));
		Map<AiReviewCategory, RestaurantAiCategoryComparison> categoryDetails = parseCategoryComparisons(
			asStringObjectMap(comparisonJson.get("category_lift")),
			comparisonDisplay);
		return new RestaurantAiComparison(categoryDetails);
	}

	private static Map<AiReviewCategory, RestaurantAiCategorySummary> parseCategorySummaries(
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

	private static RestaurantAiCategorySummary parseCategorySummary(Object value) {
		Map<String, Object> map = asStringObjectMap(value);
		if (map == null) {
			return null;
		}

		return new RestaurantAiCategorySummary(
			toNonBlankString(map.get("summary")),
			parseStrings(map.get("bullets")),
			parseEvidences(map.get("evidence")));
	}

	private static Map<AiReviewCategory, RestaurantAiCategoryComparison> parseCategoryComparisons(
		Map<String, Object> categoryLift,
		List<String> comparisonDisplay) {
		if (categoryLift == null || categoryLift.isEmpty()) {
			return Map.of();
		}

		Map<AiReviewCategory, RestaurantAiCategoryComparison> result = new LinkedHashMap<>();
		for (int i = 0; i < COMPARISON_DISPLAY_ORDER.size(); i++) {
			AiReviewCategory category = COMPARISON_DISPLAY_ORDER.get(i);
			result.put(category, new RestaurantAiCategoryComparison(
				i < comparisonDisplay.size() ? comparisonDisplay.get(i) : null,
				List.of(),
				List.of(),
				toDouble(categoryLift.get(category.getJsonKey()))));
		}
		return result;
	}

	private static List<String> parseStrings(Object value) {
		if (!(value instanceof List<?> list)) {
			return List.of();
		}

		return list.stream()
			.map(RestaurantAiJsonParser::toNonBlankString)
			.filter(text -> text != null)
			.toList();
	}

	private static List<RestaurantAiEvidence> parseEvidences(Object value) {
		if (!(value instanceof List<?> list)) {
			return List.of();
		}

		return list.stream()
			.limit(MAX_EVIDENCES)
			.map(RestaurantAiJsonParser::asStringObjectMap)
			.filter(map -> map != null)
			.map(evidence -> new RestaurantAiEvidence(
				parseReviewId(evidence.get("review_id")),
				toNonBlankString(evidence.get("snippet")),
				null,
				null,
				null))
			.toList();
	}

	private static Object firstPresent(Map<String, Object> source, String... keys) {
		for (String key : keys) {
			Object value = source.get(key);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asStringObjectMap(Object value) {
		if (!(value instanceof Map<?, ?> map)) {
			return null;
		}
		return (Map<String, Object>)map;
	}

	private static Long parseReviewId(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Double toDouble(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.doubleValue();
		}
		try {
			return Double.parseDouble(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static String toNonBlankString(Object value) {
		if (value == null) {
			return null;
		}

		String text = value.toString();
		return text.isBlank() ? null : text;
	}
}
