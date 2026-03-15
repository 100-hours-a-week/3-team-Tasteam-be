package com.tasteam.domain.search.repository.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;

import jakarta.persistence.Query;

/**
 * native SQL 기반 검색 전략 실행기의 공통 로직을 담은 추상 클래스.
 * <p>
 * - {@link #runNative}: 6컬럼 결과(ftsRank=null)를 처리하는 일반 native 실행기
 * - {@link #runFtsNative}: 7컬럼 결과(fts_rank 포함)를 처리하는 FTS 전용 실행기
 * - {@link #CURSOR_WHERE_AND_ORDER}: 5개 native 전략이 공유하는 커서 페이지네이션 SQL 절
 */
public abstract class NativeSearchExecutorSupport extends QueryDslSupport implements SearchQueryExecutor {

	protected static final int HYBRID_LIMIT_MULTIPLIER = 3;

	/**
	 * 커서 페이지네이션 WHERE/ORDER/LIMIT 공통 SQL 절.
	 * 각 Executor의 SQL 빌더 마지막에 {@code + CURSOR_WHERE_AND_ORDER}로 이어 붙인다.
	 */
	protected static final String CURSOR_WHERE_AND_ORDER = """
		WHERE (
		    CAST(:cursor_score AS double precision) IS NULL
		    OR total_score < CAST(:cursor_score AS double precision)
		    OR (total_score = CAST(:cursor_score AS double precision) AND updated_at < CAST(:cursor_updated_at AS timestamptz))
		    OR (total_score = CAST(:cursor_score AS double precision) AND updated_at = CAST(:cursor_updated_at AS timestamptz) AND restaurant_id < CAST(:cursor_id AS bigint))
		)
		ORDER BY total_score DESC, updated_at DESC, restaurant_id DESC
		LIMIT :size
		""";

	protected NativeSearchExecutorSupport() {
		super(Restaurant.class);
	}

	/**
	 * 일반 native 전략 실행기.
	 * 결과 컬럼 순서: restaurant_id, name_exact, name_similarity, distance_meters, category_match, address_match
	 * ftsRank는 null로 고정된다.
	 */
	@SuppressWarnings("unchecked")
	protected List<SearchRestaurantCursorRow> runNative(String sql, String keyword, SearchCursor cursor, int size,
		Double latitude, Double longitude, Double radiusMeters, int textCandidateLimit, int geoCandidateLimit) {
		Query query = getEntityManager().createNativeQuery(sql);
		String keywordLower = keyword.toLowerCase();
		Double cursorScore = cursor == null ? null : cursorScore(cursor, radiusMeters);

		query.setParameter("kw", keywordLower);
		query.setParameter("size", size);
		if (sql.contains(":text_candidate_limit")) {
			query.setParameter("text_candidate_limit", Math.max(size, textCandidateLimit));
		}
		if (sql.contains(":geo_candidate_limit")) {
			query.setParameter("geo_candidate_limit", Math.max(size, geoCandidateLimit));
		}
		query.setParameter("cursor_score", cursorScore);
		query.setParameter("cursor_updated_at", cursor == null ? null : cursor.updatedAt());
		query.setParameter("cursor_id", cursor == null ? null : cursor.id());
		if (sql.contains(":lat")) {
			query.setParameter("lat", latitude);
		}
		if (sql.contains(":lng")) {
			query.setParameter("lng", longitude);
		}
		if (sql.contains(":radius_m")) {
			query.setParameter("radius_m", radiusMeters);
		}

		List<Object[]> rows = query.getResultList();
		if (rows.isEmpty()) {
			return List.of();
		}

		Map<Long, Restaurant> restaurantMap = fetchRestaurantMap(rows.stream()
			.map(row -> toLong(row[0]))
			.toList());

		List<SearchRestaurantCursorRow> result = new ArrayList<>();
		for (Object[] row : rows) {
			Long restaurantId = toLong(row[0]);
			Restaurant restaurant = restaurantMap.get(restaurantId);
			if (restaurant == null) {
				continue;
			}
			// columns: restaurant_id, name_exact, name_similarity, distance_meters, category_match, address_match
			result.add(new SearchRestaurantCursorRow(
				restaurant,
				toInteger(row[1]),
				toDouble(row[2]),
				null,
				toNullableDouble(row[3]),
				toInteger(row[4]),
				toInteger(row[5])));
		}
		return result;
	}

	/**
	 * FTS 전용 native 실행기.
	 * 결과 컬럼 순서: restaurant_id, name_exact, name_similarity, fts_rank, distance_meters, category_match, address_match
	 */
	@SuppressWarnings("unchecked")
	protected List<SearchRestaurantCursorRow> runFtsNative(String sql, String keyword, SearchCursor cursor, int size,
		Double latitude, Double longitude, Double radiusMeters, int textCandidateLimit) {
		Query query = getEntityManager().createNativeQuery(sql);
		String keywordLower = keyword.toLowerCase();
		Double cursorScore = cursor == null ? null : cursorScoreFts(cursor, radiusMeters);

		query.setParameter("kw", keywordLower);
		query.setParameter("size", size);
		query.setParameter("text_candidate_limit", Math.max(size, textCandidateLimit));
		query.setParameter("cursor_score", cursorScore);
		query.setParameter("cursor_updated_at", cursor == null ? null : cursor.updatedAt());
		query.setParameter("cursor_id", cursor == null ? null : cursor.id());
		if (sql.contains(":lat")) {
			query.setParameter("lat", latitude);
		}
		if (sql.contains(":lng")) {
			query.setParameter("lng", longitude);
		}
		if (sql.contains(":radius_m")) {
			query.setParameter("radius_m", radiusMeters);
		}

		List<Object[]> rows = query.getResultList();
		if (rows.isEmpty()) {
			return List.of();
		}

		Map<Long, Restaurant> restaurantMap = fetchRestaurantMap(rows.stream()
			.map(row -> toLong(row[0]))
			.toList());

		List<SearchRestaurantCursorRow> result = new ArrayList<>();
		for (Object[] row : rows) {
			Long restaurantId = toLong(row[0]);
			Restaurant restaurant = restaurantMap.get(restaurantId);
			if (restaurant == null) {
				continue;
			}
			// columns: restaurant_id, name_exact, name_similarity, fts_rank, distance_meters, category_match, address_match
			result.add(new SearchRestaurantCursorRow(
				restaurant,
				toInteger(row[1]),
				toDouble(row[2]),
				toNullableDouble(row[3]),
				toNullableDouble(row[4]),
				toInteger(row[5]),
				toInteger(row[6])));
		}
		return result;
	}

	/**
	 * FTS 제외 커서 점수 계산 (name*100 + similarity*30 + distanceWeight*50).
	 * ONE_STEP, TWO_STEP, JOIN_AGGREGATE, HYBRID_*, READ_MODEL_*, MV_SINGLE_PASS 전략에서 사용.
	 */
	protected double cursorScore(SearchCursor cursor, Double radiusMeters) {
		return SearchScoreCalculator.cursorScore(cursor, radiusMeters);
	}

	/**
	 * FTS 포함 커서 점수 계산 (name*100 + similarity*30 + ftsRank*25 + category*15 + address*5 + distanceWeight*50).
	 * FTS_MV_RANKED 전략에서 사용.
	 */
	protected double cursorScoreFts(SearchCursor cursor, Double radiusMeters) {
		return SearchScoreCalculator.cursorScoreFts(cursor, radiusMeters);
	}

	private Map<Long, Restaurant> fetchRestaurantMap(List<Long> ids) {
		QRestaurant r = QRestaurant.restaurant;
		Map<Long, Restaurant> map = new HashMap<>();
		getQueryFactory()
			.selectFrom(r)
			.where(r.id.in(ids))
			.fetch()
			.forEach(restaurant -> map.put(restaurant.getId(), restaurant));
		return map;
	}

	protected Long toLong(Object value) {
		return ((Number)value).longValue();
	}

	protected Integer toInteger(Object value) {
		return value == null ? 0 : ((Number)value).intValue();
	}

	protected Double toDouble(Object value) {
		return value == null ? 0.0 : ((Number)value).doubleValue();
	}

	protected Double toNullableDouble(Object value) {
		return value == null ? null : ((Number)value).doubleValue();
	}

	// QueryDslSupport가 요구하는 NumberExpression 유틸 — native Executor에서는 직접 사용하지 않으나
	// 일부 서브클래스가 QueryDSL 표현식을 보조로 활용할 수 있도록 노출한다.
	protected static NumberExpression<Double> nullDouble() {
		return Expressions.numberTemplate(Double.class, "NULL");
	}
}
