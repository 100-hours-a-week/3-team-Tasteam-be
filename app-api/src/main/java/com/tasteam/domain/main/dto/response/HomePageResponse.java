package com.tasteam.domain.main.dto.response;

import java.util.List;

public record HomePageResponse(List<Section> sections) {

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
		String reviewSummary) {
	}
}
