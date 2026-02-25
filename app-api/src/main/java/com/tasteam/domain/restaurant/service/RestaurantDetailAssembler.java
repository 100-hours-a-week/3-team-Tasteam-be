package com.tasteam.domain.restaurant.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tasteam.domain.restaurant.dto.RestaurantAiCategoryComparison;
import com.tasteam.domain.restaurant.dto.RestaurantAiCategorySummary;
import com.tasteam.domain.restaurant.dto.RestaurantAiComparison;
import com.tasteam.domain.restaurant.dto.RestaurantAiDetails;
import com.tasteam.domain.restaurant.dto.RestaurantAiEvidence;
import com.tasteam.domain.restaurant.dto.RestaurantAiSentiment;
import com.tasteam.domain.restaurant.dto.RestaurantAiSummary;
import com.tasteam.domain.restaurant.dto.response.AiCategoryComparisonResponse;
import com.tasteam.domain.restaurant.dto.response.AiCategorySummaryResponse;
import com.tasteam.domain.restaurant.dto.response.AiEvidenceResponse;
import com.tasteam.domain.restaurant.dto.response.BusinessHourWeekItem;
import com.tasteam.domain.restaurant.dto.response.RestaurantAiComparisonResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantAiDetailsResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantAiSentimentResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantAiSummaryResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.service.RestaurantImageService.RestaurantPrimaryImage;
import com.tasteam.domain.restaurant.service.RestaurantReadService.RestaurantReadResult;

@Component
public class RestaurantDetailAssembler {

	public RestaurantDetailResponse assemble(
		RestaurantReadResult readResult,
		List<BusinessHourWeekItem> businessHoursWeek,
		RestaurantAiDetails aiDetails,
		RestaurantPrimaryImage primaryImage) {
		Restaurant restaurant = readResult.restaurant();
		RestaurantImageDto image = primaryImage == null
			? null
			: new RestaurantImageDto(primaryImage.imageId(), primaryImage.url());

		RestaurantAiDetailsResponse aiDetailsResponse = aiDetails != null ? toAiDetailsResponse(aiDetails) : null;

		return new RestaurantDetailResponse(
			restaurant.getId(),
			restaurant.getName(),
			restaurant.getFullAddress(),
			restaurant.getPhoneNumber(),
			readResult.foodCategories(),
			businessHoursWeek,
			image,
			null,
			readResult.recommendedCount(),
			aiDetailsResponse,
			restaurant.getCreatedAt(),
			restaurant.getUpdatedAt());
	}

	private RestaurantAiDetailsResponse toAiDetailsResponse(RestaurantAiDetails aiDetails) {
		return new RestaurantAiDetailsResponse(
			toSentimentResponse(aiDetails.sentiment()),
			toSummaryResponse(aiDetails.summary()),
			toComparisonResponse(aiDetails.comparison()));
	}

	private RestaurantAiSentimentResponse toSentimentResponse(RestaurantAiSentiment sentiment) {
		if (sentiment == null) {
			return null;
		}
		return new RestaurantAiSentimentResponse(sentiment.positivePercent());
	}

	private RestaurantAiSummaryResponse toSummaryResponse(RestaurantAiSummary summary) {
		if (summary == null) {
			return null;
		}
		Map<String, AiCategorySummaryResponse> categoryDetails = summary.categoryDetails() == null
			? Map.of()
			: summary.categoryDetails().entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().name(), e -> toCategorySummaryResponse(e.getValue())));
		return new RestaurantAiSummaryResponse(summary.overallSummary(), categoryDetails);
	}

	private AiCategorySummaryResponse toCategorySummaryResponse(RestaurantAiCategorySummary detail) {
		if (detail == null) {
			return null;
		}
		List<AiEvidenceResponse> evidences = detail.evidences() == null
			? List.of()
			: detail.evidences().stream().map(this::toEvidenceResponse).toList();
		return new AiCategorySummaryResponse(
			detail.summary(),
			detail.bullets() != null ? detail.bullets() : List.of(),
			evidences);
	}

	private RestaurantAiComparisonResponse toComparisonResponse(RestaurantAiComparison comparison) {
		if (comparison == null) {
			return null;
		}
		Map<String, AiCategoryComparisonResponse> categoryDetails = comparison.categoryDetails() == null
			? Map.of()
			: comparison.categoryDetails().entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().name(), e -> toCategoryComparisonResponse(e.getValue())));
		return new RestaurantAiComparisonResponse(comparison.overallComparison(), categoryDetails);
	}

	private AiCategoryComparisonResponse toCategoryComparisonResponse(RestaurantAiCategoryComparison detail) {
		if (detail == null) {
			return null;
		}
		List<AiEvidenceResponse> evidences = detail.evidences() == null
			? List.of()
			: detail.evidences().stream().map(this::toEvidenceResponse).toList();
		return new AiCategoryComparisonResponse(
			detail.summary(),
			detail.bullets() != null ? detail.bullets() : List.of(),
			evidences,
			detail.liftScore());
	}

	private AiEvidenceResponse toEvidenceResponse(RestaurantAiEvidence evidence) {
		if (evidence == null) {
			return null;
		}
		return new AiEvidenceResponse(evidence.reviewId(), evidence.snippet());
	}
}
