package com.tasteam.domain.search.repository.executor.nativesql;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.NativeSearchExecutorSupport;
import com.tasteam.domain.search.repository.executor.SearchQueryExecutor;

/**
 * [MV_SINGLE_PASS] restaurant_search_mv를 단일 스캔으로 필터링·스코어링·정렬까지 한 번에 처리하는 전략.
 *
 * <p>{@link ReadModelTwoStepExecutor}와 달리 CTE 교집합 단계 없이 WHERE 절에서 직접 텍스트·지오 조건을 적용하고, 인라인으로 스코어를
 * 계산한 뒤 LIMIT로 후보를 잘라낸다.
 *
 * <p>흐름: MV WHERE(텍스트 OR + 지오 필터) → 인라인 스코어 계산 → ORDER BY 스코어 DESC LIMIT candidateLimit →
 * 커서 페이징 → restaurant ID로 엔티티 일괄 조회
 *
 * <p>장점: CTE 없이 단일 패스이므로 쿼리 계획이 단순하고 MV 시퀀셜 스캔에 유리하다. 단점: candidateLimit 이후 커서 WHERE를 한 번 더
 * 적용하므로 실제 반환 수가 size보다 적을 수 있다.
 */
@Component
public class MvSinglePassExecutor extends NativeSearchExecutorSupport implements SearchQueryExecutor {

	private static final int HYBRID_LIMIT_MULTIPLIER = 3;

	private final SearchQueryProperties properties;

	public MvSinglePassExecutor(SearchQueryProperties properties) {
		this.properties = properties;
	}

	@Override
	public SearchQueryStrategy strategy() {
		return SearchQueryStrategy.MV_SINGLE_PASS;
	}

	@Override
	public List<SearchRestaurantCursorRow> execute(String keyword, SearchCursor cursor, int size, Double latitude,
		Double longitude, Double radiusMeters) {
		int candidateLimit = properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER;
		return runNative(
			buildSql(latitude != null && longitude != null && radiusMeters != null),
			keyword, cursor, size, latitude, longitude, radiusMeters,
			candidateLimit, candidateLimit);
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
			+ geoFilter + """
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
				""" + CURSOR_WHERE_AND_ORDER;
	}
}
