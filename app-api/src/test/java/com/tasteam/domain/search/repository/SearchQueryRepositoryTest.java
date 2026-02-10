package com.tasteam.domain.search.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;

@RepositoryJpaTest
class SearchQueryRepositoryTest {

	private static final double LAT = 37.5;
	private static final double LON = 126.9;
	private static final double RADIUS_METERS = 3000.0;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private SearchQueryRepository searchQueryRepository;

	private GeometryFactory geometryFactory;

	@BeforeEach
	void setUp() {
		geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
	}

	@Test
	@DisplayName("이름 조건 + 거리 조건 + 점수 정렬이 요구조건과 일치한다")
	void searchRestaurantsMatchesSpec() {
		Restaurant exact = createRestaurant("치킨", point(LON, LAT));
		Restaurant similar = createRestaurant("치킨킹", point(LON + 0.002, LAT + 0.002));
		Restaurant other = createRestaurant("피자", point(LON, LAT));
		Restaurant outside = createRestaurant("치킨먼곳", point(LON + 0.5, LAT + 0.5));
		Restaurant deleted = createRestaurant("치킨삭제", point(LON, LAT));
		deleted.softDelete(Instant.now());

		restaurantRepository.saveAll(List.of(exact, similar, other, outside, deleted));
		restaurantRepository.flush();

		List<Long> expectedIds = jdbcTemplate.queryForList("""
			SELECT r.id
			FROM restaurant r
			WHERE r.deleted_at IS NULL
			  AND (lower(r.name) LIKE '%치킨%' OR lower(r.name) % '치킨')
			  AND ST_DistanceSphere(r.location, ST_MakePoint(?, ?)) <= ?
			ORDER BY (
			  (CASE WHEN lower(r.name) = '치킨' THEN 1 ELSE 0 END) * 100
			  + similarity(lower(r.name), '치킨') * 30
			  + GREATEST(0, 1 - ST_DistanceSphere(r.location, ST_MakePoint(?, ?)) / ?) * 50
			) DESC,
			 r.updated_at DESC,
			 r.id DESC
			LIMIT 20
			""", Long.class, LON, LAT, RADIUS_METERS, LON, LAT, RADIUS_METERS);

		List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
			"치킨",
			null,
			20,
			LAT,
			LON,
			RADIUS_METERS);

		List<Long> actualIds = result.stream()
			.map(row -> row.restaurant().getId())
			.collect(Collectors.toList());

		assertThat(actualIds).containsExactlyElementsOf(expectedIds);
		assertThat(actualIds).doesNotContain(other.getId(), outside.getId(), deleted.getId());
	}

	private Restaurant createRestaurant(String name, Point location) {
		return Restaurant.create(
			name,
			"서울특별시 강남구 테헤란로 123",
			location,
			"02-7777-8888");
	}

	private Point point(double lon, double lat) {
		return geometryFactory.createPoint(new Coordinate(lon, lat));
	}
}
