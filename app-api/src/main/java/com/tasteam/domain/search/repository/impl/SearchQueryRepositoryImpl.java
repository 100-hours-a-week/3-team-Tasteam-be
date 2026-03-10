package com.tasteam.domain.search.repository.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.restaurant.entity.QFoodCategory;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.restaurant.entity.QRestaurantFoodCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.SearchQueryProperties;
import com.tasteam.domain.search.repository.SearchQueryRepository;
import com.tasteam.domain.search.repository.SearchQueryStrategy;

import jakarta.persistence.Query;

@Repository
public class SearchQueryRepositoryImpl extends QueryDslSupport implements SearchQueryRepository {

	private final SearchQueryProperties properties;
	private static final double MIN_NAME_SIMILARITY = 0.3;
	private static final int HYBRID_LIMIT_MULTIPLIER = 3;

	public SearchQueryRepositoryImpl(SearchQueryProperties properties) {
		super(Restaurant.class);
		this.properties = properties;
	}

	@Override
	public List<SearchRestaurantCursorRow> searchRestaurantsByKeyword(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		if (properties.getStrategy() == SearchQueryStrategy.HYBRID_SPLIT_CANDIDATES) {
			return searchHybridSplitCandidates(keyword, cursor, size, latitude, longitude, radiusMeters);
		}
		if (properties.getStrategy() == SearchQueryStrategy.GEO_FIRST_HYBRID) {
			return searchGeoFirstHybrid(keyword, cursor, size, latitude, longitude, radiusMeters);
		}
		if (properties.getStrategy() == SearchQueryStrategy.READ_MODEL_TWO_STEP) {
			return searchReadModelTwoStep(keyword, cursor, size, latitude, longitude, radiusMeters);
		}
		if (properties.getStrategy() == SearchQueryStrategy.MV_SINGLE_PASS) {
			return searchMvSinglePass(keyword, cursor, size, latitude, longitude, radiusMeters);
		}
		if (properties.getStrategy() == SearchQueryStrategy.TWO_STEP) {
			return searchTwoStep(keyword, cursor, size, latitude, longitude, radiusMeters);
		}
		if (properties.getStrategy() == SearchQueryStrategy.JOIN_AGGREGATE) {
			return searchJoinAggregate(keyword, cursor, size, latitude, longitude, radiusMeters);
		}
		return searchOneStep(keyword, cursor, size, latitude, longitude, radiusMeters);
	}

	private List<SearchRestaurantCursorRow> searchHybridSplitCandidates(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		return executeNativeStrategy(
			buildHybridSplitSql(latitude != null && longitude != null && radiusMeters != null),
			keyword,
			cursor,
			size,
			latitude,
			longitude,
			radiusMeters,
			properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER,
			properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER);
	}

	private List<SearchRestaurantCursorRow> searchGeoFirstHybrid(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		if (latitude == null || longitude == null || radiusMeters == null) {
			return searchHybridSplitCandidates(keyword, cursor, size, latitude, longitude, radiusMeters);
		}
		return executeNativeStrategy(
			buildGeoFirstSql(),
			keyword,
			cursor,
			size,
			latitude,
			longitude,
			radiusMeters,
			properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER,
			properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER);
	}

	private List<SearchRestaurantCursorRow> searchReadModelTwoStep(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		return executeNativeStrategy(
			buildReadModelSql(latitude != null && longitude != null && radiusMeters != null),
			keyword,
			cursor,
			size,
			latitude,
			longitude,
			radiusMeters,
			properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER,
			properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER);
	}

	@SuppressWarnings("unchecked")
	private List<SearchRestaurantCursorRow> executeNativeStrategy(String sql, String keyword, SearchCursor cursor,
		int size,
		Double latitude,
		Double longitude,
		Double radiusMeters,
		int textCandidateLimit,
		int geoCandidateLimit) {
		Query query = getEntityManager().createNativeQuery(sql);
		String keywordLower = keyword.toLowerCase();
		Double cursorScore = cursor == null ? null : cursorScore(cursor, radiusMeters);

		query.setParameter("kw", keywordLower);
		query.setParameter("size", size);
		query.setParameter("text_candidate_limit", Math.max(size, textCandidateLimit));
		query.setParameter("geo_candidate_limit", Math.max(size, geoCandidateLimit));
		query.setParameter("cursor_score", cursorScore);
		query.setParameter("cursor_updated_at", cursor == null ? null : cursor.updatedAt());
		query.setParameter("cursor_id", cursor == null ? null : cursor.id());
		query.setParameter("lat", latitude);
		query.setParameter("lng", longitude);
		query.setParameter("radius_m", radiusMeters);

		List<Object[]> rows = query.getResultList();
		if (rows.isEmpty()) {
			return List.of();
		}

		List<Long> restaurantIds = rows.stream()
			.map(row -> toLong(row[0]))
			.toList();

		QRestaurant r = QRestaurant.restaurant;
		Map<Long, Restaurant> restaurantMap = new HashMap<>();
		getQueryFactory()
			.selectFrom(r)
			.where(r.id.in(restaurantIds))
			.fetch()
			.forEach(restaurant -> restaurantMap.put(restaurant.getId(), restaurant));

		List<SearchRestaurantCursorRow> result = new ArrayList<>();
		for (Object[] row : rows) {
			Long restaurantId = toLong(row[0]);
			Restaurant restaurant = restaurantMap.get(restaurantId);
			if (restaurant == null) {
				continue;
			}
			result.add(new SearchRestaurantCursorRow(
				restaurant,
				toInteger(row[1]),
				toDouble(row[2]),
				toNullableDouble(row[3]),
				toInteger(row[4]),
				toInteger(row[5])));
		}
		return result;
	}

	private String buildHybridSplitSql(boolean withLocation) {
		String geoCte = withLocation
			? """
				, geo_candidates AS (
				    SELECT r.id
				    FROM restaurant r
				    WHERE r.deleted_at IS NULL
				      AND ST_DWithin(geography(r.location), geography(ST_MakePoint(:lng, :lat)), :radius_m)
				    ORDER BY ST_DistanceSphere(r.location, ST_MakePoint(:lng, :lat)) ASC, r.updated_at DESC, r.id DESC
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
			? "ST_DistanceSphere(r.location, ST_MakePoint(:lng, :lat))"
			: "NULL::double precision";

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
			""" + geoCte + """
			, scored_base AS (
			    SELECT
			        r.id AS restaurant_id,
			        CASE WHEN lower(r.name) = :kw THEN 1 ELSE 0 END AS name_exact,
			        similarity(lower(r.name), :kw)::double precision AS name_similarity,
			"""
			+ distanceExpr +
			"""
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
				        name_exact,
				        name_similarity,
				        distance_meters,
				        category_match,
				        address_match,
				        updated_at,
				        (name_exact * 100.0)
				            + (name_similarity * 30.0)
				            + CASE
				                WHEN distance_meters IS NULL OR :radius_m IS NULL THEN 0.0
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
				WHERE (
				    :cursor_score IS NULL
				    OR total_score < :cursor_score
				    OR (total_score = :cursor_score AND updated_at < :cursor_updated_at)
				    OR (total_score = :cursor_score AND updated_at = :cursor_updated_at AND restaurant_id < :cursor_id)
				)
				ORDER BY total_score DESC, updated_at DESC, restaurant_id DESC
				LIMIT :size
				""";
	}

	private String buildGeoFirstSql() {
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
			WHERE (
			    :cursor_score IS NULL
			    OR total_score < :cursor_score
			    OR (total_score = :cursor_score AND updated_at < :cursor_updated_at)
			    OR (total_score = :cursor_score AND updated_at = :cursor_updated_at AND restaurant_id < :cursor_id)
			)
			ORDER BY total_score DESC, updated_at DESC, restaurant_id DESC
			LIMIT :size
			""";
	}

	private String buildReadModelSql(boolean withLocation) {
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
			: "NULL::double precision";

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
			+ distanceExpr +
			"""
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
				                WHEN distance_meters IS NULL OR :radius_m IS NULL THEN 0.0
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
				WHERE (
				    :cursor_score IS NULL
				    OR total_score < :cursor_score
				    OR (total_score = :cursor_score AND updated_at < :cursor_updated_at)
				    OR (total_score = :cursor_score AND updated_at = :cursor_updated_at AND restaurant_id < :cursor_id)
				)
				ORDER BY total_score DESC, updated_at DESC, restaurant_id DESC
				LIMIT :size
				""";
	}

	private List<SearchRestaurantCursorRow> searchMvSinglePass(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		return executeNativeStrategy(
			buildMvSinglePassSql(latitude != null && longitude != null && radiusMeters != null),
			keyword,
			cursor,
			size,
			latitude,
			longitude,
			radiusMeters,
			properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER,
			properties.getCandidateLimit() * HYBRID_LIMIT_MULTIPLIER);
	}

	private String buildMvSinglePassSql(boolean withLocation) {
		String geoFilter = withLocation
			? "AND ST_DWithin(geography(mv.location), geography(ST_MakePoint(:lng, :lat)), :radius_m)"
			: "";
		String distanceExpr = withLocation
			? "ST_DistanceSphere(mv.location, ST_MakePoint(:lng, :lat))"
			: "NULL::double precision";
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
				WHERE (
				    :cursor_score IS NULL
				    OR total_score < :cursor_score
				    OR (total_score = :cursor_score AND updated_at < :cursor_updated_at)
				    OR (total_score = :cursor_score AND updated_at = :cursor_updated_at AND restaurant_id < :cursor_id)
				)
				ORDER BY total_score DESC, updated_at DESC, restaurant_id DESC
				LIMIT :size
				""";
	}

	private Long toLong(Object value) {
		return ((Number)value).longValue();
	}

	private Integer toInteger(Object value) {
		return value == null ? 0 : ((Number)value).intValue();
	}

	private Double toDouble(Object value) {
		return value == null ? 0.0 : ((Number)value).doubleValue();
	}

	private Double toNullableDouble(Object value) {
		return value == null ? null : ((Number)value).doubleValue();
	}

	private List<SearchRestaurantCursorRow> searchOneStep(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		QRestaurant r = QRestaurant.restaurant;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression nameSimilar = nameSimilar(r, kw);
		BooleanExpression categoryExists = categoryMatchExists(kw, r);
		NumberExpression<Integer> nameExactScore = nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = categoryMatchScore(categoryExists);
		NumberExpression<Integer> addressScore = addressMatchScore(r, kw);
		NumberExpression<Double> totalScore = totalScore(nameExactScore, nameSimilarity, distanceExpr, radiusMeters);

		return getQueryFactory()
			.select(Projections.constructor(
				SearchRestaurantCursorRow.class,
				r,
				nameExactScore,
				nameSimilarity,
				distanceExpr,
				categoryScore,
				addressScore))
			.from(r)
			.where(
				r.deletedAt.isNull(),
				nameContains.or(nameSimilar),
				distanceFilter(r, latitude, longitude, radiusMeters),
				cursorCondition(cursor, r, totalScore, radiusMeters))
			.orderBy(scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}

	private List<SearchRestaurantCursorRow> searchTwoStep(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		QRestaurant r = QRestaurant.restaurant;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression nameSimilar = nameSimilar(r, kw);
		BooleanExpression categoryExists = categoryMatchExists(kw, r);
		NumberExpression<Integer> nameExactScore = nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = categoryMatchScore(categoryExists);
		NumberExpression<Integer> addressScore = addressMatchScore(r, kw);
		NumberExpression<Double> totalScore = totalScore(nameExactScore, nameSimilarity, distanceExpr, radiusMeters);

		List<Long> candidateIds = getQueryFactory()
			.select(r.id)
			.from(r)
			.where(
				r.deletedAt.isNull(),
				nameContains.or(nameSimilar),
				distanceFilter(r, latitude, longitude, radiusMeters),
				cursorCondition(cursor, r, totalScore, radiusMeters))
			.orderBy(scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(properties.getCandidateLimit())
			.fetch();

		if (candidateIds.isEmpty()) {
			return List.of();
		}

		return getQueryFactory()
			.select(Projections.constructor(
				SearchRestaurantCursorRow.class,
				r,
				nameExactScore,
				nameSimilarity,
				distanceExpr,
				categoryScore,
				addressScore))
			.from(r)
			.where(
				r.id.in(candidateIds),
				cursorCondition(cursor, r, totalScore, radiusMeters))
			.orderBy(scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}

	private List<SearchRestaurantCursorRow> searchJoinAggregate(String keyword, SearchCursor cursor, int size,
		Double latitude,
		Double longitude,
		Double radiusMeters) {
		QRestaurant r = QRestaurant.restaurant;
		QRestaurantFoodCategory rfc = QRestaurantFoodCategory.restaurantFoodCategory;
		QFoodCategory fc = QFoodCategory.foodCategory;
		String kw = keyword.toLowerCase();

		BooleanExpression nameContains = r.name.lower().contains(kw);
		BooleanExpression nameSimilar = nameSimilar(r, kw);
		NumberExpression<Integer> nameExactScore = nameExactScore(r, kw);
		NumberExpression<Double> nameSimilarity = nameSimilarity(r, kw);
		NumberExpression<Double> distanceExpr = distanceMeters(r, latitude, longitude);
		NumberExpression<Integer> categoryScore = categoryMatchAggregate(fc, kw);
		NumberExpression<Integer> addressScore = addressMatchScore(r, kw);
		NumberExpression<Double> totalScore = totalScore(nameExactScore, nameSimilarity, distanceExpr, radiusMeters);

		return getQueryFactory()
			.select(Projections.constructor(
				SearchRestaurantCursorRow.class,
				r,
				nameExactScore,
				nameSimilarity,
				distanceExpr,
				categoryScore,
				addressScore))
			.from(r)
			.leftJoin(rfc).on(rfc.restaurant.eq(r))
			.leftJoin(rfc.foodCategory, fc)
			.where(
				r.deletedAt.isNull(),
				nameContains.or(nameSimilar),
				distanceFilter(r, latitude, longitude, radiusMeters),
				cursorCondition(cursor, r, totalScore, radiusMeters))
			.groupBy(r.id)
			.orderBy(scoreOrderSpecifiers(r, totalScore).toArray(OrderSpecifier[]::new))
			.limit(size)
			.fetch();
	}

	private List<OrderSpecifier<?>> scoreOrderSpecifiers(QRestaurant r, NumberExpression<Double> totalScore) {
		List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
		orderSpecifiers.add(totalScore.desc());
		orderSpecifiers.add(r.updatedAt.desc());
		orderSpecifiers.add(r.id.desc());
		return orderSpecifiers;
	}

	private NumberExpression<Integer> nameExactScore(QRestaurant r, String keywordLower) {
		return new CaseBuilder()
			.when(r.name.lower().eq(keywordLower))
			.then(1)
			.otherwise(0);
	}

	private NumberExpression<Double> nameSimilarity(QRestaurant r, String keywordLower) {
		return Expressions.numberTemplate(Double.class,
			"cast(function('similarity', lower({0}), lower({1})) as double)", r.name, keywordLower);
	}

	private BooleanExpression nameSimilar(QRestaurant r, String keywordLower) {
		return nameSimilarity(r, keywordLower).goe(MIN_NAME_SIMILARITY);
	}

	private NumberExpression<Double> distanceMeters(QRestaurant r, Double latitude, Double longitude) {
		if (latitude == null || longitude == null) {
			return Expressions.numberTemplate(Double.class, "NULL");
		}
		return Expressions.numberTemplate(Double.class,
			"ST_DistanceSphere({0}, ST_MakePoint({1}, {2}))", r.location, longitude, latitude);
	}

	private NumberExpression<Integer> addressMatchScore(QRestaurant r, String keywordLower) {
		return new CaseBuilder()
			.when(r.fullAddress.lower().contains(keywordLower))
			.then(1)
			.otherwise(0);
	}

	private NumberExpression<Integer> categoryMatchScore(BooleanExpression categoryExists) {
		return new CaseBuilder()
			.when(categoryExists)
			.then(1)
			.otherwise(0);
	}

	private NumberExpression<Integer> categoryMatchAggregate(QFoodCategory fc, String keywordLower) {
		return Expressions.numberTemplate(Integer.class,
			"max(case when lower({0}) = {1} then 1 else 0 end)", fc.name, keywordLower);
	}

	private BooleanExpression categoryMatchExists(String keywordLower, QRestaurant r) {
		QRestaurantFoodCategory rfc = QRestaurantFoodCategory.restaurantFoodCategory;
		QFoodCategory fc = QFoodCategory.foodCategory;

		return JPAExpressions.selectOne()
			.from(rfc)
			.join(rfc.foodCategory, fc)
			.where(
				rfc.restaurant.eq(r),
				fc.name.lower().eq(keywordLower))
			.exists();
	}

	private NumberExpression<Double> totalScore(NumberExpression<Integer> nameExactScore,
		NumberExpression<Double> nameSimilarity, NumberExpression<Double> distanceMeters, Double radiusMeters) {
		NumberExpression<Double> distanceWeight = distanceWeight(distanceMeters, radiusMeters);
		return nameExactScore.doubleValue()
			.multiply(100.0)
			.add(nameSimilarity.multiply(30.0))
			.add(distanceWeight.multiply(50.0));
	}

	private NumberExpression<Double> distanceWeight(NumberExpression<Double> distanceMeters, Double radiusMeters) {
		if (radiusMeters == null) {
			return Expressions.numberTemplate(Double.class, "0.0");
		}
		return Expressions.numberTemplate(Double.class,
			"cast(greatest(0.0, 1.0 - (cast({0} as double) / cast({1} as double))) as double)",
			distanceMeters, radiusMeters);
	}

	private BooleanExpression distanceFilter(QRestaurant r, Double latitude, Double longitude, Double radiusMeters) {
		if (radiusMeters == null || latitude == null || longitude == null) {
			return null;
		}
		return Expressions.numberTemplate(Integer.class,
			"function('st_dwithin_geo', {0}, {1}, {2}, {3})",
			r.location, longitude, latitude, radiusMeters).eq(1);
	}

	private BooleanExpression cursorCondition(SearchCursor cursor, QRestaurant r,
		NumberExpression<Double> totalScore, Double radiusMeters) {
		if (cursor == null) {
			return null;
		}

		double cursorScore = cursorScore(cursor, radiusMeters);
		BooleanExpression cond = null;
		cond = or(cond, totalScore.lt(cursorScore));
		cond = or(cond, totalScore.eq(cursorScore).and(r.updatedAt.lt(cursor.updatedAt())));
		cond = or(cond, totalScore.eq(cursorScore)
			.and(r.updatedAt.eq(cursor.updatedAt()))
			.and(r.id.lt(cursor.id())));

		return cond;
	}

	private double cursorScore(SearchCursor cursor, Double radiusMeters) {
		double nameExact = cursor.nameExact() == null ? 0.0 : cursor.nameExact();
		double similarity = cursor.nameSimilarity() == null ? 0.0 : cursor.nameSimilarity();
		double distanceWeight = 0.0;
		if (cursor.distanceMeters() != null && radiusMeters != null) {
			double effectiveRadius = Math.max(radiusMeters, 1.0);
			distanceWeight = Math.max(0.0, 1.0 - (cursor.distanceMeters() / effectiveRadius));
		}
		return nameExact * 100.0 + similarity * 30.0 + distanceWeight * 50.0;
	}

	private BooleanExpression or(BooleanExpression base, BooleanExpression next) {
		return base == null ? next : base.or(next);
	}
}
