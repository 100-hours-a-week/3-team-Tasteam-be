package com.tasteam.domain.analytics.export;

import java.util.List;

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RawDataExportSourceJdbcRepository {

	private static final int FETCH_SIZE = 500;

	// Contract(4-2) restaurants schema: restaurant_id,restaurant_name,sido,sigungu,eupmyeondong,geohash,food_category_id,food_category_name
	private static final List<String> RESTAURANT_HEADERS = List.of(
		"restaurant_id", "restaurant_name", "sido", "sigungu", "eupmyeondong", "geohash", "food_category_id",
		"food_category_name");

	// Contract(4-3) menus schema: restaurant_id,menu_count,price_min,price_max,price_mean,price_median,representative_menu_name,top_menus,price_tier
	private static final List<String> MENU_HEADERS = List.of(
		"restaurant_id", "menu_count", "price_min", "price_max", "price_mean", "price_median",
		"representative_menu_name", "top_menus", "price_tier");

	private static final String SELECT_RESTAURANTS_SQL = """
		SELECT
			r.id::text AS restaurant_id,
			r.name AS restaurant_name,
			COALESCE(ra.sido, '') AS sido,
			COALESCE(ra.sigungu, '') AS sigungu,
			COALESCE(ra.eupmyeondong, '') AS eupmyeondong,
			COALESCE(ST_GeoHash(r.location, 8), '') AS geohash,
			COALESCE(fc.id::text, '') AS food_category_id,
			COALESCE(fc.name, '') AS food_category_name
		FROM restaurant r
		LEFT JOIN restaurant_address ra ON ra.restaurant_id = r.id
		LEFT JOIN restaurant_food_category rfc ON rfc.restaurant_id = r.id
		LEFT JOIN food_category fc ON fc.id = rfc.food_category_id
		ORDER BY r.id ASC, fc.id ASC
		""";

	private static final String SELECT_MENUS_SQL = """
		WITH menu_base AS (
			SELECT
				m.id,
				m.name,
				m.price,
				COALESCE(m.is_recommended, false) AS is_recommended,
				COALESCE(m.display_order, 2147483647) AS display_order,
				mc.restaurant_id
			FROM menu m
			JOIN menu_category mc ON m.category_id = mc.id
		),
		menu_ranked AS (
			SELECT
				mb.*,
				ROW_NUMBER() OVER (
					PARTITION BY mb.restaurant_id
					ORDER BY
						CASE WHEN mb.is_recommended THEN 0 ELSE 1 END,
						mb.display_order ASC,
						mb.id ASC
				) AS menu_rank
			FROM menu_base mb
		)
		SELECT
			mr.restaurant_id::text AS restaurant_id,
			COUNT(*)::text AS menu_count,
			MIN(mr.price)::text AS price_min,
			MAX(mr.price)::text AS price_max,
			ROUND(AVG(mr.price)::numeric, 2)::text AS price_mean,
			ROUND(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY mr.price)::numeric, 2)::text AS price_median,
			COALESCE(MAX(CASE WHEN mr.menu_rank = 1 THEN mr.name END), '') AS representative_menu_name,
			-- top_menus rule: 추천 우선/노출순/ID순으로 정렬된 상위 5개를 JSON 배열로 저장
			COALESCE(
				jsonb_agg(
					jsonb_build_object(
						"name", mr.name,
						"price", mr.price
					)
					ORDER BY mr.menu_rank
				) FILTER (WHERE mr.menu_rank <= 5),
				'[]'::jsonb
			)::text AS top_menus,
			-- price_tier rule: 평균 가격 기준 고정 버킷
			CASE
				WHEN AVG(mr.price) < 10000 THEN 'UNDER_10000'
				WHEN AVG(mr.price) < 20000 THEN 'UNDER_20000'
				WHEN AVG(mr.price) < 30000 THEN 'UNDER_30000'
				ELSE 'OVER_30000'
			END AS price_tier
		FROM menu_ranked mr
		GROUP BY mr.restaurant_id
		ORDER BY mr.restaurant_id ASC
		""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public RawDataExportSourceJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<String> restaurantHeaders() {
		return RESTAURANT_HEADERS;
	}

	public List<String> menuHeaders() {
		return MENU_HEADERS;
	}

	public void streamRestaurants(CsvRowConsumer consumer) {
		jdbcTemplate.getJdbcTemplate().query(
			connection -> {
				var statement = connection.prepareStatement(SELECT_RESTAURANTS_SQL);
				statement.setFetchSize(FETCH_SIZE);
				return statement;
			},
			(RowCallbackHandler)rs -> consumer.accept(List.of(
				rs.getString(1),
				rs.getString(2),
				rs.getString(3),
				rs.getString(4),
				rs.getString(5),
				rs.getString(6),
				rs.getString(7),
				rs.getString(8))));
	}

	public void streamMenus(CsvRowConsumer consumer) {
		jdbcTemplate.getJdbcTemplate().query(
			connection -> {
				var statement = connection.prepareStatement(SELECT_MENUS_SQL);
				statement.setFetchSize(FETCH_SIZE);
				return statement;
			},
			(RowCallbackHandler)rs -> consumer.accept(List.of(
				rs.getString(1),
				rs.getString(2),
				rs.getString(3),
				rs.getString(4),
				rs.getString(5),
				rs.getString(6),
				rs.getString(7),
				rs.getString(8),
				rs.getString(9))));
	}
}
