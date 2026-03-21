package com.tasteam.domain.main.dto.response;

import java.util.List;

public record AiRecommendResponse(Section section) {

	public record Section(
		String type,
		String title,
		List<MainSectionItem> items) {
	}
}
