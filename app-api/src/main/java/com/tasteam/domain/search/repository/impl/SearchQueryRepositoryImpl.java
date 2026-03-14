package com.tasteam.domain.search.repository.impl;

import static java.util.function.Function.*;
import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryRepository;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.SearchQueryExecutor;

/**
 * 검색 전략 라우터.
 * 활성 전략({@link SearchQueryProperties#getStrategy()})에 맞는 {@link SearchQueryExecutor}를
 * Map에서 찾아 실행한다. 전략이 등록되지 않은 경우 {@link SearchQueryStrategy#ONE_STEP}으로 폴백한다.
 * <p>
 * 새 전략은 {@link SearchQueryExecutor}를 구현하고 {@code @Component}를 붙이면 자동 등록된다.
 */
@Repository
public class SearchQueryRepositoryImpl implements SearchQueryRepository {

	private final SearchQueryProperties properties;
	private final Map<SearchQueryStrategy, SearchQueryExecutor> executorMap;

	public SearchQueryRepositoryImpl(SearchQueryProperties properties, List<SearchQueryExecutor> executors) {
		this.properties = properties;
		this.executorMap = executors.stream()
			.collect(toUnmodifiableMap(SearchQueryExecutor::strategy, identity()));
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
