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
		String geoFilter = NativeSqlFragments.geoFilter(withLocation);
		String distanceExpr = withLocation
			? "ST_DistanceSphere(mv.location, ST_MakePoint(:lng, :lat))"
			: "NULL::double precision";
		String distanceScore = withLocation
			? "GREATEST(0.0, 1.0 - (distance_meters / :radius_m)) * 50.0"
			: "0.0";

		// tsq CTE: plainto_tsquery를 한 번만 평가해 filtered/scored에서 재사용
		// filtered CTE: 인덱스 조건만으로 후보 추출 + ST_DistanceSphere 1회 계산
		// scored CTE: 비용 함수(similarity, ts_rank_cd, CASE WHEN)를 각 1회 계산
		// ranked CTE: 이미 계산된 컬럼으로 total_score 조합 후 LIMIT
		return "WITH tsq AS ("
			+ " SELECT plainto_tsquery('simple', :kw) AS q"
			+ " ),"
			+ " filtered AS ("
			+ "     SELECT"
			+ "         mv.restaurant_id,"
			+ "         mv.name,"
			+ "         mv.full_address,"
			+ "         mv.updated_at,"
			+ "         mv.name_lower,"
			+ "         mv.search_vector,"
			+ "         mv.category_names,"
			+ "         mv.addr_lower,"
			+ "         " + distanceExpr + " AS distance_meters"
			+ "     FROM restaurant_search_mv mv, tsq"
			+ "     WHERE mv.deleted_at IS NULL"
			+ "       " + geoFilter
			+ "       AND ("
			+ "             mv.name_lower LIKE '%' || :kw || '%'"
			+ "             OR mv.name_lower % :kw"
			+ "             OR mv.search_vector @@ tsq.q"
			+ "             OR mv.category_names @> ARRAY[:kw]::text[]"
			+ "           )"
			+ " ),"
			+ " scored AS ("
			+ "     SELECT"
			+ "         f.restaurant_id,"
			+ "         f.name,"
			+ "         f.full_address,"
			+ "         f.updated_at,"
			+ "         CASE WHEN f.name_lower = :kw THEN 1 ELSE 0 END                      AS name_exact,"
			+ "         similarity(f.name_lower, :kw)::double precision                      AS name_similarity,"
			+ "         ts_rank_cd(f.search_vector, tsq.q)::double precision                 AS fts_rank,"
			+ "         f.distance_meters,"
			+ "         CASE WHEN f.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END  AS category_match,"
			+ "         CASE WHEN f.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END     AS address_match"
			+ "     FROM filtered f, tsq"
			+ " ),"
			+ " ranked AS ("
			+ "     SELECT"
			+ "         restaurant_id,"
			+ "         name,"
			+ "         full_address,"
			+ "         updated_at,"
			+ "         name_exact,"
			+ "         name_similarity,"
			+ "         fts_rank,"
			+ "         distance_meters,"
			+ "         category_match,"
			+ "         address_match,"
			+ "         (name_exact * 100.0"
			+ "          + name_similarity * 30.0"
			+ "          + fts_rank * 25.0"
			+ "          + category_match * 15.0"
			+ "          + address_match * 5.0"
			+ "          + " + distanceScore + ") AS total_score"
			+ "     FROM scored"
			+ "     ORDER BY total_score DESC, updated_at DESC, restaurant_id DESC"
			+ "     LIMIT :text_candidate_limit"
			+ " )"
			+ " SELECT restaurant_id, name, full_address, name_exact, name_similarity, fts_rank,"
			+ "        distance_meters, category_match, address_match, updated_at"
			+ " FROM ranked"
			+ " " + CURSOR_WHERE_AND_ORDER;
	}
}
