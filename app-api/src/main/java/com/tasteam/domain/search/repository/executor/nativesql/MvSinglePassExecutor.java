package com.tasteam.domain.search.repository.executor.nativesql;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.NativeSearchExecutorSupport;
import com.tasteam.domain.search.repository.executor.NativeSqlFragments;

/**
 * MV_SINGLE_PASS 전략 실행기.
 * restaurant_search_mv 뷰를 단일 패스로 스캔해 텍스트 조건·스코어링·정렬을 한 번에 처리한다.
 * 별도의 후보 수집 없이 뷰의 인덱스를 직접 활용하므로 쿼리가 단순하다.
 */
@Component
public class MvSinglePassExecutor extends NativeSearchExecutorSupport {

	private final SearchQueryProperties properties;

	public MvSinglePassExecutor(SearchQueryProperties properties) {
		this.properties = properties;
	}

	@Override
	public SearchQueryStrategy strategy() {
		return SearchQueryStrategy.MV_SINGLE_PASS;
	}

	@Override
	public List<SearchRestaurantCursorRow> execute(String keyword, SearchCursor cursor, int size,
		Double latitude, Double longitude, Double radiusMeters) {
		boolean withLocation = latitude != null && longitude != null && radiusMeters != null;
		int limit = properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER;
		return runNative(buildSql(withLocation), keyword, cursor, size,
			latitude, longitude, radiusMeters, limit, limit);
	}

	private String buildSql(boolean withLocation) {
		String geoFilter = NativeSqlFragments.geoFilter(withLocation);
		String distanceExpr = NativeSqlFragments.distanceExprMv(withLocation);
		String distanceScore = NativeSqlFragments.distanceScoreMv(withLocation);

		return """
			WITH candidates AS (
			    SELECT
			        mv.restaurant_id,
			        mv.updated_at,
			        CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END AS name_exact,
			        similarity(mv.name_lower, :kw)::double precision AS name_similarity,
			        """
			+ distanceExpr + """
				AS distance_meters,
				CASE WHEN mv.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END AS category_match,
				CASE WHEN mv.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END AS address_match,
				(
				    CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END * 100.0
				    + similarity(mv.name_lower, :kw)::double precision * 30.0
				    + """
			+ distanceScore + """
				) AS total_score
				FROM restaurant_search_mv mv
				WHERE mv.deleted_at IS NULL
				  """
			+ geoFilter
			+ """
				      AND (
				            mv.name_lower LIKE '%' || :kw || '%'
				            OR mv.name_lower % :kw
				            OR mv.addr_lower LIKE '%' || :kw || '%'
				            OR mv.category_names @> ARRAY[:kw]::text[]
				          )
				    ORDER BY total_score DESC, mv.updated_at DESC, mv.restaurant_id DESC
				    LIMIT :text_candidate_limit
				)
				SELECT
				    restaurant_id,
				    name_exact,
				    name_similarity,
				    distance_meters,
				    category_match,
				    address_match
				FROM candidates
				"""
			+ CURSOR_WHERE_AND_ORDER;
	}
}
