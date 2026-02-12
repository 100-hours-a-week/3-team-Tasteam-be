package com.tasteam.domain.restaurant.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.domain.restaurant.dto.response.BusinessHourWeekItem;
import com.tasteam.domain.restaurant.dto.response.RestaurantDetailResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantDetailResponse.RecommendStatResponse;
import com.tasteam.domain.restaurant.dto.response.RestaurantImageDto;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.service.RestaurantAiSummaryService.RestaurantAiSummaryResult;
import com.tasteam.domain.restaurant.service.RestaurantImageService.RestaurantPrimaryImage;
import com.tasteam.domain.restaurant.service.RestaurantReadService.RestaurantReadResult;

@Component
public class RestaurantDetailAssembler {

	public RestaurantDetailResponse assemble(
		RestaurantReadResult readResult,
		List<BusinessHourWeekItem> businessHoursWeek,
		RestaurantAiSummaryResult aiSummaryResult,
		RestaurantPrimaryImage primaryImage) {
		Restaurant restaurant = readResult.restaurant();
		RestaurantImageDto image = primaryImage == null
			? null
			: new RestaurantImageDto(primaryImage.imageId(), primaryImage.url());

		RecommendStatResponse recommendStatResponse = new RecommendStatResponse(
			readResult.recommendedCount(),
			readResult.notRecommendedCount(),
			aiSummaryResult.positiveRatio());

		return new RestaurantDetailResponse(
			restaurant.getId(),
			restaurant.getName(),
			restaurant.getFullAddress(),
			restaurant.getPhoneNumber(),
			readResult.foodCategories(),
			businessHoursWeek,
			image,
			null,
			recommendStatResponse,
			aiSummaryResult.summary(),
			aiSummaryResult.feature(),
			restaurant.getCreatedAt(),
			restaurant.getUpdatedAt());
	}
}
