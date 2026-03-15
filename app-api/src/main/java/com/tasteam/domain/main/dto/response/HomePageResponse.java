package com.tasteam.domain.main.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tasteam.domain.promotion.dto.response.SplashPromotionResponse;

public record HomePageResponse(
	MainPageResponse.Banners banners,
	List<Section> sections,

	@JsonInclude(JsonInclude.Include.NON_NULL)
	SplashPromotionResponse splashPromotion) {

	public record Section(
		String type,
		String title,
		List<MainSectionItem> items) {
	}
}
