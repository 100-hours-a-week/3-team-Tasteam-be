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
