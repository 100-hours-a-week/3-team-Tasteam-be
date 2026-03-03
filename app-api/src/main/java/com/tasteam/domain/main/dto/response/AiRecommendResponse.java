package com.tasteam.domain.main.dto.response;

import java.util.List;

public record AiRecommendResponse(Section section) {

	public record Section(
		String type,
		String title,
		List<SectionItem> items) {
	}

	public record SectionItem(
		Long restaurantId,
		String name,
		Double distanceMeter,
		List<String> foodCategories,
		String thumbnailImageUrl,
		String reviewSummary) {
	}
}
