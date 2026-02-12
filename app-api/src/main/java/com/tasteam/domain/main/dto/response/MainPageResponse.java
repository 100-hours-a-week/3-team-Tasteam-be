package com.tasteam.domain.main.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tasteam.domain.promotion.dto.response.SplashPromotionResponse;

public record MainPageResponse(
	Banners banners,
	List<Section> sections,

	@JsonInclude(JsonInclude.Include.NON_NULL)
	SplashPromotionResponse splashPromotion) {

	public record Banners(
		boolean enabled,
		List<BannerItem> items) {
	}

	public record BannerItem(
		Long id,
		String imageUrl,
		String landingUrl,
		Integer order) {
	}

	public record Section(
		String type,
		String title,
		List<SectionItem> items) {
	}

	public record SectionItem(
		Long restaurantId,
		String name,
		Double distanceMeter,
		String category,
		String thumbnailImageUrl,
		Boolean isFavorite,
		String reviewSummary) {
	}
}
