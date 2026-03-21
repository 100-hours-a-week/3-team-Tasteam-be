package com.tasteam.domain.search.repository.executor;

import java.util.List;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryStrategy;

/**
 * 검색 전략 실행기 인터페이스.
 * 각 구현체는 하나의 {@link SearchQueryStrategy}에 대응하며,
 * {@link com.tasteam.domain.search.repository.impl.SearchQueryRepositoryImpl}에서
 * Map 기반 라우팅으로 자동 디스패치된다.
 */
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
