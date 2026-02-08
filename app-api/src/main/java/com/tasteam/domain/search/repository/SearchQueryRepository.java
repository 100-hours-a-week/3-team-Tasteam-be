package com.tasteam.domain.search.repository;

import java.util.List;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;

public interface SearchQueryRepository {

	List<SearchRestaurantCursorRow> searchRestaurantsByKeyword(String keyword, SearchCursor cursor, int size,
		Double latitude, Double longitude);
}
