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
		@JsonInclude(JsonInclude.Include.NON_NULL)
		List<MainSectionItem> items,

		@JsonInclude(JsonInclude.Include.NON_NULL)
		List<Group> groups) {

		public static Section items(String type, String title, List<MainSectionItem> items) {
			return new Section(type, title, items, null);
		}

		public static Section groups(String type, String title, List<Group> groups) {
			return new Section(type, title, null, groups);
		}
	}

	public record Group(
		String category,
		String title,
		List<MainSectionItem> items) {
	}
}
