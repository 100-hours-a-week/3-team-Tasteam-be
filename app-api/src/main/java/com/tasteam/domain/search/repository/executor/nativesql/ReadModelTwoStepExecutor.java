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
 * [READ_MODEL_TWO_STEP] 별도 읽기 모델(restaurant_search_mv)을 사용하는 텍스트+지오 교집합 전략.
 *
 * <p>{@link HybridSplitExecutor}와 구조가 동일하지만, restaurant 원본 테이블 대신 사전 집계된 MV를 조회한다. MV에는
 * name_lower·addr_lower·category_names 컬럼이 미리 계산되어 있어 JOIN 없이 텍스트 필터가 가능하다.
 *
 * <p>흐름:
 *
 * <ol>
 *   <li>text_candidates — MV의 name_lower·addr_lower·category_names로 LIKE·유사도·배열 포함 검색 후 UNION
 *   <li>geo_candidates — MV의 location으로 반경 필터 (위치 없으면 생략)
 *   <li>candidate_ids — INTERSECT 또는 text_candidates 전체
 *   <li>scored — MV에서 스코어 계산 후 커서 페이징 적용 → restaurant ID로 엔티티 일괄 조회
 * </ol>
 *
 * <p>장점: 원본 테이블 JOIN·lower() 함수 호출 없이 MV의 인덱스를 직접 활용해 텍스트 검색 비용을 줄인다. 단점: MV 동기화 지연 가능성이
 * 있으며, restaurant_search_mv에 JPA 엔티티가 없어 native SQL로 유지된다.
 */
@Component
public class ReadModelTwoStepExecutor extends NativeSearchExecutorSupport implements SearchQueryExecutor {

	private static final int HYBRID_LIMIT_MULTIPLIER = 3;

	private final SearchQueryProperties properties;

	public ReadModelTwoStepExecutor(SearchQueryProperties properties) {
		this.properties = properties;
	}

	@Override
	public SearchQueryStrategy strategy() {
		return SearchQueryStrategy.READ_MODEL_TWO_STEP;
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
		String geoCte = withLocation
			? """
				, geo_candidates AS (
				    SELECT mv.restaurant_id AS id
				    FROM restaurant_search_mv mv
				    WHERE mv.deleted_at IS NULL
				      AND ST_DWithin(geography(mv.location), geography(ST_MakePoint(:lng, :lat)), :radius_m)
				    ORDER BY ST_DistanceSphere(mv.location, ST_MakePoint(:lng, :lat)) ASC, mv.updated_at DESC, mv.restaurant_id DESC
				    LIMIT :geo_candidate_limit
				)
				, candidate_ids AS (
				    SELECT id FROM text_candidates
				    INTERSECT
				    SELECT id FROM geo_candidates
				)
				"""
			: """
				, candidate_ids AS (
				    SELECT DISTINCT id
				    FROM text_candidates
				)
				""";

		String distanceExpr = withLocation
			? "ST_DistanceSphere(mv.location, ST_MakePoint(:lng, :lat))"
			: "NULL::double precision ";

		String distanceScore = withLocation
			? "GREATEST(0.0, 1.0 - (distance_meters / :radius_m)) * 50.0"
			: "0.0";

		return """
			WITH name_like_candidates AS (
			    SELECT mv.restaurant_id AS id
			    FROM restaurant_search_mv mv
			    WHERE mv.deleted_at IS NULL
			      AND mv.name_lower LIKE '%' || :kw || '%'
			    ORDER BY similarity(mv.name_lower, :kw) DESC, mv.updated_at DESC, mv.restaurant_id DESC
			    LIMIT :text_candidate_limit
			), name_similarity_candidates AS (
			    SELECT mv.restaurant_id AS id
			    FROM restaurant_search_mv mv
			    WHERE mv.deleted_at IS NULL
			      AND mv.name_lower % :kw
			    ORDER BY similarity(mv.name_lower, :kw) DESC, mv.updated_at DESC, mv.restaurant_id DESC
			    LIMIT :text_candidate_limit
			), address_candidates AS (
			    SELECT mv.restaurant_id AS id
			    FROM restaurant_search_mv mv
			    WHERE mv.deleted_at IS NULL
			      AND mv.addr_lower LIKE '%' || :kw || '%'
			    ORDER BY mv.updated_at DESC, mv.restaurant_id DESC
			    LIMIT :text_candidate_limit
			), category_candidates AS (
			    SELECT mv.restaurant_id AS id
			    FROM restaurant_search_mv mv
			    WHERE mv.deleted_at IS NULL
			      AND mv.category_names @> ARRAY[:kw]::text[]
			    ORDER BY mv.updated_at DESC, mv.restaurant_id DESC
			    LIMIT :text_candidate_limit
			), text_candidates AS (
			    SELECT id FROM name_like_candidates
			    UNION
			    SELECT id FROM name_similarity_candidates
			    UNION
			    SELECT id FROM address_candidates
			    UNION
			    SELECT id FROM category_candidates
			)
			""" + geoCte + """
			, scored_base AS (
			    SELECT
			        mv.restaurant_id,
			        CASE WHEN mv.name_lower = :kw THEN 1 ELSE 0 END AS name_exact,
			        similarity(mv.name_lower, :kw)::double precision AS name_similarity,
			        """
			+ distanceExpr + """
					AS distance_meters,
					CASE WHEN mv.category_names @> ARRAY[:kw]::text[] THEN 1 ELSE 0 END AS category_match,
					CASE WHEN mv.addr_lower LIKE '%' || :kw || '%' THEN 1 ELSE 0 END AS address_match,
					mv.updated_at
				FROM restaurant_search_mv mv
				JOIN candidate_ids c ON c.id = mv.restaurant_id
				WHERE mv.deleted_at IS NULL
				), scored AS (
				    SELECT
				        restaurant_id,
				        name_exact,
				        name_similarity,
				        distance_meters,
				        category_match,
				        address_match,
				        updated_at,
				        (name_exact * 100.0)
				            + (name_similarity * 30.0)
				            + CASE
				                WHEN distance_meters IS NULL OR CAST(:radius_m AS double precision) IS NULL THEN 0.0
				                ELSE """
			+ distanceScore + """
				              END AS total_score
				    FROM scored_base
				)
				SELECT
				    restaurant_id,
				    name_exact,
				    name_similarity,
				    distance_meters,
				    category_match,
				    address_match
				FROM scored
				""" + CURSOR_WHERE_AND_ORDER;
	}
}
