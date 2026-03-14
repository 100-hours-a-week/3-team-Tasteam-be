package com.tasteam.domain.search.repository.executor.nativesql;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.NativeSearchExecutorSupport;

/**
 * FTS_MV_RANKED 전략 실행기.
 * restaurant_search_mv 뷰의 search_vector(tsvector) 컬럼을 활용해
 * ts_rank_cd() 기반 풀텍스트 스코어를 점수식에 포함한다.
 * <p>
 * 점수식: name_exact*100 + name_similarity*30 + fts_rank*25 + category_match*15 + address_match*5 + distance*50
 * <p>
 * 위치 파라미터가 null인 경우 거리 필터·점수를 생략한 쿼리를 사용한다.
 */
@Component
public class FtsMvRankedExecutor extends NativeSearchExecutorSupport {

	private final SearchQueryProperties properties;

	public FtsMvRankedExecutor(SearchQueryProperties properties) {
		this.properties = properties;
	}

	@Override
	public SearchQueryStrategy strategy() {
		return SearchQueryStrategy.FTS_MV_RANKED;
	}

	@Override
	public List<SearchRestaurantCursorRow> execute(String keyword, SearchCursor cursor, int size,
		Double latitude, Double longitude, Double radiusMeters) {
		boolean withLocation = latitude != null && longitude != null && radiusMeters != null;
		return runFtsNative(
			buildSql(withLocation),
			keyword, cursor, size, latitude, longitude, radiusMeters,
			properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER);
	}

	private String buildSql(boolean withLocation) {
		String geoFilter = withLocation
			? "AND ST_DWithin(geography(mv.location), geography(ST_MakePoint(:lng, :lat)), :radius_m)"
			: "";
		String distanceExpr = withLocation
			? "ST_DistanceSphere(mv.location, ST_MakePoint(:lng, :lat))"
			: "NULL::double precision ";
		String distanceScore = withLocation
			? "GREATEST(0.0, 1.0 - (ST_DistanceSphere(mv.location, ST_MakePoint(:lng, :lat)) / :radius_m)) * 50.0"
			: "0.0";

		return """
			WITH candidates AS (
			    SELECT
			        mv.restaurant_id,
			        mv.updated_at,
			        CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END                                               AS name_exact,
			        similarity(mv.name_lower, :kw)::double precision                                               AS name_similarity,
			        ts_rank_cd(mv.search_vector, plainto_tsquery('simple', :kw))::double precision                 AS fts_rank,
			        """
			+ distanceExpr
			+ """
				AS distance_meters,
				CASE WHEN mv.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END                           AS category_match,
				CASE WHEN mv.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END                              AS address_match,
				(
				    CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END * 100.0
				    + similarity(mv.name_lower, :kw)::double precision * 30.0
				    + ts_rank_cd(mv.search_vector, plainto_tsquery('simple', :kw))::double precision * 25.0
				    + CASE WHEN mv.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END * 15.0
				    + CASE WHEN mv.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END * 5.0
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
				            OR mv.search_vector @@ plainto_tsquery('simple', :kw)
				            OR mv.category_names @> ARRAY[:kw]::text[]
				          )
				    ORDER BY total_score DESC, mv.updated_at DESC, mv.restaurant_id DESC
				    LIMIT :text_candidate_limit
				)
				SELECT
				    restaurant_id,
				    name_exact,
				    name_similarity,
				    fts_rank,
				    distance_meters,
				    category_match,
				    address_match
				FROM candidates
				"""
			+ CURSOR_WHERE_AND_ORDER;
	}
}
