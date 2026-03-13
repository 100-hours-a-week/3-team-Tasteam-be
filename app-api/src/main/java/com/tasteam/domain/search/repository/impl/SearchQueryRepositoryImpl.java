package com.tasteam.domain.search.repository.impl;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryRepository;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.SearchQueryExecutor;

@Repository
public class SearchQueryRepositoryImpl implements SearchQueryRepository {

	private final Map<SearchQueryStrategy, SearchQueryExecutor> executorMap;
	private final SearchQueryProperties properties;

	public SearchQueryRepositoryImpl(List<SearchQueryExecutor> executors, SearchQueryProperties properties) {
		this.executorMap = executors.stream()
			.collect(toUnmodifiableMap(SearchQueryExecutor::strategy, identity()));
		this.properties = properties;
	}

	@Override
	public List<SearchRestaurantCursorRow> searchRestaurantsByKeyword(String keyword, SearchCursor cursor, int size,
		Double latitude, Double longitude, Double radiusMeters) {
		SearchQueryExecutor executor = executorMap.getOrDefault(
			properties.getStrategy(),
			executorMap.get(SearchQueryStrategy.ONE_STEP));
		return executor.execute(keyword, cursor, size, latitude, longitude, radiusMeters);
	}
}
