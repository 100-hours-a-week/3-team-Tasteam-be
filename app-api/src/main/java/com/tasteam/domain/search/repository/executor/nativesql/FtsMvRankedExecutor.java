package com.tasteam.domain.search.repository.executor.nativesql;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
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
			keyword, cursor, size, latitude, longitude, radiusMeters, 0);
	}

	private String buildSql(boolean withLocation) {
		String geoFilter = NativeSqlFragments.geoFilter(withLocation);
		String distanceExpr = withLocation
			? "ST_Distance(mv.location_geo, geography(ST_MakePoint(:lng, :lat)))"
			: "NULL::double precision";
		String distanceScore = withLocation
			? "GREATEST(0.0, 1.0 - (distance_meters / :radius_m)) * 50.0"
			: "0.0";

		// tsq CTE: plainto_tsquery를 한 번만 평가해 filtered/scored에서 재사용
		// filtered CTE: 인덱스 조건만으로 후보 추출 + 거리 1회 계산
		// scored_raw CTE: similarity, ts_rank_cd 등 비용 함수를 각각 1회만 계산
		// scored CTE: scored_raw의 이미 계산된 컬럼을 참조해 total_score 산출 (중복 호출 없음)
		// ranked CTE: 이미 계산된 total_score로 커서 조건 적용 후 LIMIT :size
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
			+ " scored_raw AS ("
			+ "     SELECT"
			+ "         f.restaurant_id,"
			+ "         f.name,"
			+ "         f.full_address,"
			+ "         f.updated_at,"
			+ "         f.distance_meters,"
			+ "         CASE WHEN f.name_lower = :kw THEN 1 ELSE 0 END                      AS name_exact,"
			+ "         similarity(f.name_lower, :kw)::double precision                      AS name_similarity,"
			+ "         ts_rank_cd(f.search_vector, tsq.q)::double precision                 AS fts_rank,"
			+ "         CASE WHEN f.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END  AS category_match,"
			+ "         CASE WHEN f.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END     AS address_match"
			+ "     FROM filtered f, tsq"
			+ " ),"
			+ " scored AS ("
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
			+ "          + " + distanceScore + ")                                            AS total_score"
			+ "     FROM scored_raw"
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
			+ "         total_score"
			+ "     FROM scored"
			+ "     WHERE ("
			+ "         CAST(:cursor_score AS double precision) IS NULL"
			+ "         OR total_score < CAST(:cursor_score AS double precision)"
			+ "         OR (total_score = CAST(:cursor_score AS double precision) AND updated_at < CAST(:cursor_updated_at AS timestamptz))"
			+ "         OR (total_score = CAST(:cursor_score AS double precision) AND updated_at = CAST(:cursor_updated_at AS timestamptz) AND restaurant_id < CAST(:cursor_id AS bigint))"
			+ "     )"
			+ "     ORDER BY total_score DESC, updated_at DESC, restaurant_id DESC"
			+ "     LIMIT :size"
			+ " )"
			+ " SELECT restaurant_id, name, full_address, name_exact, name_similarity, fts_rank,"
			+ "        distance_meters, category_match, address_match, updated_at"
			+ " FROM ranked"
			+ " ORDER BY total_score DESC, updated_at DESC, restaurant_id DESC";
	}
}
