package com.tasteam.domain.search.repository.executor.nativesql;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.NativeSearchExecutorSupport;

/**
 * READ_MODEL_TWO_STEP 전략 실행기.
 * restaurant_search_mv 뷰를 대상으로 텍스트·지리 후보를 각각 수집한 뒤 INTERSECT로 교집합을 취한다.
 * 뷰에 이미 집계된 category_names, addr_lower 컬럼을 활용해 restaurant 테이블 접근을 최소화한다.
 */
@Component
public class ReadModelTwoStepExecutor extends NativeSearchExecutorSupport {

	private final SearchQueryProperties properties;

	public ReadModelTwoStepExecutor(SearchQueryProperties properties) {
		this.properties = properties;
	}

	@Override
	public SearchQueryStrategy strategy() {
		return SearchQueryStrategy.READ_MODEL_TWO_STEP;
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
			"""
			+ geoCte + """
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
				                ELSE GREATEST(0.0, 1.0 - (distance_meters / :radius_m)) * 50.0
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
				"""
			+ CURSOR_WHERE_AND_ORDER;
	}
}
