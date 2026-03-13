package com.tasteam.domain.search.repository.executor.nativesql;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryStrategy;
import com.tasteam.domain.search.repository.executor.NativeSearchExecutorSupport;
import com.tasteam.domain.search.repository.executor.SearchQueryExecutor;

@Component
public class GeoFirstHybridExecutor extends NativeSearchExecutorSupport implements SearchQueryExecutor {

	private static final int HYBRID_LIMIT_MULTIPLIER = 3;

	private final SearchQueryProperties properties;
	private final HybridSplitExecutor fallback;

	public GeoFirstHybridExecutor(SearchQueryProperties properties, HybridSplitExecutor fallback) {
		this.properties = properties;
		this.fallback = fallback;
	}

	@Override
	public SearchQueryStrategy strategy() {
		return SearchQueryStrategy.GEO_FIRST_HYBRID;
	}

	@Override
	public List<SearchRestaurantCursorRow> execute(String keyword, SearchCursor cursor, int size, Double latitude,
		Double longitude, Double radiusMeters) {
		if (latitude == null || longitude == null || radiusMeters == null) {
			return fallback.execute(keyword, cursor, size, latitude, longitude, radiusMeters);
		}
		int candidateLimit = properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER;
		return runNative(
			buildSql(),
			keyword, cursor, size, latitude, longitude, radiusMeters,
			candidateLimit, candidateLimit);
	}

	private String buildSql() {
		return """
			WITH geo_candidates AS (
			    SELECT
			        r.id,
			        ST_DistanceSphere(r.location, ST_MakePoint(:lng, :lat)) AS distance_meters
			    FROM restaurant r
			    WHERE r.deleted_at IS NULL
			      AND ST_DWithin(geography(r.location), geography(ST_MakePoint(:lng, :lat)), :radius_m)
			    ORDER BY distance_meters ASC, r.updated_at DESC, r.id DESC
			    LIMIT :geo_candidate_limit
			), text_candidates AS (
			    SELECT DISTINCT g.id, g.distance_meters
			    FROM geo_candidates g
			    JOIN restaurant r ON r.id = g.id
			    WHERE
			        lower(r.name) LIKE '%%' || :kw || '%%'
			        OR lower(r.name) % :kw
			        OR lower(r.full_address) LIKE '%' || :kw || '%'
			        OR EXISTS (
			            SELECT 1
			            FROM restaurant_food_category rfc2
			            JOIN food_category fc2 ON fc2.id = rfc2.food_category_id
			            WHERE rfc2.restaurant_id = r.id
			              AND lower(fc2.name) = :kw
			        )
			), scored_base AS (
			    SELECT
			        r.id AS restaurant_id,
			        CASE WHEN lower(r.name) = :kw THEN 1 ELSE 0 END AS name_exact,
			        similarity(lower(r.name), :kw)::double precision AS name_similarity,
			        t.distance_meters::double precision AS distance_meters,
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
			    JOIN text_candidates t ON t.id = r.id
			    WHERE r.deleted_at IS NULL
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
			            + GREATEST(0.0, 1.0 - (distance_meters / :radius_m)) * 50.0 AS total_score
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
