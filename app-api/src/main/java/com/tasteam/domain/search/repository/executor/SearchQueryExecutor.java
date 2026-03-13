package com.tasteam.domain.search.repository.executor;

import java.util.List;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryStrategy;

public interface SearchQueryExecutor {

	SearchQueryStrategy strategy();

	List<SearchRestaurantCursorRow> execute(
		String keyword,
		SearchCursor cursor,
		int size,
		Double latitude,
		Double longitude,
		Double radiusMeters);
}
