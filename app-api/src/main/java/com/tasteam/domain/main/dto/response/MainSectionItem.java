package com.tasteam.domain.main.dto.response;

import java.util.List;

public record MainSectionItem(
	Long restaurantId,
	String name,
	Double distanceMeter,
	List<String> foodCategories,
	String thumbnailImageUrl,
	String reviewSummary) {
}
