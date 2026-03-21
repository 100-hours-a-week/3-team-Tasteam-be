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
 * HYBRID_SPLIT_CANDIDATES 전략 실행기.
 * 텍스트(name LIKE, similarity, address, category) 후보와 지리(ST_DWithin) 후보를 각각 수집한 뒤
 * INTERSECT로 교집합을 취해 스코어링한다.
 * 위치 파라미터가 없으면 텍스트 후보만 사용한다.
 */
@Component
public class HybridSplitExecutor extends NativeSearchExecutorSupport {

	private final SearchQueryProperties properties;

	public HybridSplitExecutor(SearchQueryProperties properties) {
		this.properties = properties;
	}

	@Override
	public SearchQueryStrategy strategy() {
		return SearchQueryStrategy.HYBRID_SPLIT_CANDIDATES;
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
				    SELECT r.id
				    FROM restaurant r
				    WHERE r.deleted_at IS NULL
				      AND ST_DWithin(geography(r.location), geography(ST_MakePoint(:lng, :lat)), :radius_m)
				    ORDER BY ST_Distance(geography(r.location), geography(ST_MakePoint(:lng, :lat))) ASC, r.updated_at DESC, r.id DESC
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

		String distanceExpr = NativeSqlFragments.distanceExprRestaurant(withLocation);

		return """
			WITH name_like_candidates AS (
			    SELECT r.id
			    FROM restaurant r
			    WHERE r.deleted_at IS NULL
			      AND lower(r.name) LIKE '%' || :kw || '%'
			    ORDER BY similarity(lower(r.name), :kw) DESC, r.updated_at DESC, r.id DESC
			    LIMIT :text_candidate_limit
			), name_similarity_candidates AS (
			    SELECT r.id
			    FROM restaurant r
			    WHERE r.deleted_at IS NULL
			      AND lower(r.name) % :kw
			    ORDER BY similarity(lower(r.name), :kw) DESC, r.updated_at DESC, r.id DESC
			    LIMIT :text_candidate_limit
			), address_candidates AS (
			    SELECT r.id
			    FROM restaurant r
			    WHERE r.deleted_at IS NULL
			      AND lower(r.full_address) LIKE '%' || :kw || '%'
			    ORDER BY r.updated_at DESC, r.id DESC
			    LIMIT :text_candidate_limit
			), category_candidates AS (
			    SELECT rfc.restaurant_id AS id
			    FROM restaurant_food_category rfc
			    JOIN food_category fc ON fc.id = rfc.food_category_id
			    JOIN restaurant r ON r.id = rfc.restaurant_id
			    WHERE r.deleted_at IS NULL
			      AND lower(fc.name) = :kw
			    ORDER BY r.updated_at DESC, r.id DESC
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
				        r.id AS restaurant_id,
				        r.name,
				        r.full_address,
				        CASE WHEN lower(r.name) = :kw THEN 1 ELSE 0 END AS name_exact,
				        similarity(lower(r.name), :kw)::double precision AS name_similarity,
				        """
			+ distanceExpr + """
				        AS distance_meters,
				        CASE WHEN EXISTS (
				            SELECT 1
				            FROM restaurant_food_category rfc2
				            JOIN food_category fc2 ON fc2.id = rfc2.food_category_id
				            WHERE rfc2.restaurant_id = r.id
				              AND lower(fc2.name) = :kw
				        ) THEN 1 ELSE 0 END AS category_match,
				        CASE WHEN lower(r.full_address) LIKE '%' || :kw || '%' THEN 1 ELSE 0 END AS address_match,
				        r.updated_at
				    FROM restaurant r
				    JOIN candidate_ids c ON c.id = r.id
				    WHERE r.deleted_at IS NULL
				), scored AS (
				    SELECT
				        restaurant_id,
				        name,
				        full_address,
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
				    name,
				    full_address,
				    name_exact,
				    name_similarity,
				    distance_meters,
				    category_match,
				    address_match,
				    updated_at
				FROM scored
				"""
			+ CURSOR_WHERE_AND_ORDER;
	}
}
