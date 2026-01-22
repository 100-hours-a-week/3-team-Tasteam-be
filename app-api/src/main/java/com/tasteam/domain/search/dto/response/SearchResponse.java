package com.tasteam.domain.search.dto.response;

import java.util.List;

import com.tasteam.domain.restaurant.dto.response.CursorPageResponse;

public record SearchResponse(
	List<SearchGroupSummary> groups,
	CursorPageResponse<SearchRestaurantItem> restaurants) {

	public static SearchResponse emptyResponse() {
		return new SearchResponse(
			List.of(),
			CursorPageResponse.empty());
	}
}
