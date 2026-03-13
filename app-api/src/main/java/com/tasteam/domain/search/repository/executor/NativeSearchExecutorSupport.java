package com.tasteam.domain.search.repository.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tasteam.domain.common.repository.QueryDslSupport;
import com.tasteam.domain.restaurant.entity.QRestaurant;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;
import com.tasteam.domain.search.repository.impl.SearchQueryExpressions;

import jakarta.persistence.Query;

public abstract class NativeSearchExecutorSupport extends QueryDslSupport {

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

	@SuppressWarnings("unchecked")
	protected List<SearchRestaurantCursorRow> runNative(
		String sql,
		String keyword,
		SearchCursor cursor,
		int size,
		Double latitude,
		Double longitude,
		Double radiusMeters,
		int textCandidateLimit,
		int geoCandidateLimit) {

		Query query = getEntityManager().createNativeQuery(sql);
		String keywordLower = keyword.toLowerCase();
		Double cursorScore = cursor == null ? null : SearchQueryExpressions.cursorScore(cursor, radiusMeters);

		query.setParameter("kw", keywordLower);
		query.setParameter("size", size);
		query.setParameter("cursor_score", cursorScore);
		query.setParameter("cursor_updated_at", cursor == null ? null : cursor.updatedAt());
		query.setParameter("cursor_id", cursor == null ? null : cursor.id());
		query.setParameter("lat", latitude);
		query.setParameter("lng", longitude);
		query.setParameter("radius_m", radiusMeters);

		if (sql.contains(":text_candidate_limit")) {
			query.setParameter("text_candidate_limit", Math.max(size, textCandidateLimit));
		}
		if (sql.contains(":geo_candidate_limit")) {
			query.setParameter("geo_candidate_limit", Math.max(size, geoCandidateLimit));
		}

		return mapRows((List<Object[]>)query.getResultList());
	}

	private List<SearchRestaurantCursorRow> mapRows(List<Object[]> rows) {
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
}
