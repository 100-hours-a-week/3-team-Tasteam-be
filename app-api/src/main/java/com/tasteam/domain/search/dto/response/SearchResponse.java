package com.tasteam.domain.search.dto.response;

import java.util.List;

import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;

public record SearchResponse(SearchData data) {

	public record SearchData(
		List<SearchGroupSummary> groups,
		CursorPageResponse<SearchRestaurantItem> restaurants,
		List<SearchSectionResponse> sections) {
	}
}
