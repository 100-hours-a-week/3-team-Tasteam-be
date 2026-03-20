package com.tasteam.domain.search.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tasteam.config.annotation.RepositoryJpaTest;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.domain.search.dto.SearchCursor;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;

@RepositoryJpaTest
@DisplayName("[유닛](Search) SearchQueryRepository 단위 테스트")
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

	@Autowired
	private SearchQueryProperties searchQueryProperties;

	private GeometryFactory geometryFactory;
	private SearchQueryStrategy originalStrategy;

	@BeforeEach
	void setUp() {
		geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
		jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS postgis");
		originalStrategy = searchQueryProperties.getStrategy();
		createSearchMvIfAbsent();
	}

	private void createSearchMvIfAbsent() {
		jdbcTemplate.execute("""
			CREATE MATERIALIZED VIEW IF NOT EXISTS restaurant_search_mv AS
			SELECT
			    r.id AS restaurant_id,
			    lower(r.name)         AS name_lower,
			    lower(r.full_address) AS addr_lower,
			    r.location,
			    r.updated_at,
			    r.deleted_at,
			    COALESCE(
			        array_agg(DISTINCT lower(fc.name)) FILTER (WHERE fc.name IS NOT NULL),
			        ARRAY[]::text[]
			    ) AS category_names,
			    setweight(to_tsvector('simple', coalesce(lower(r.name), '')), 'A')
			    || setweight(to_tsvector('simple', coalesce(
			        array_to_string(
			            COALESCE(
			                array_agg(DISTINCT lower(fc.name)) FILTER (WHERE fc.name IS NOT NULL),
			                ARRAY[]::text[]
			            ), ' '), '')), 'B')
			    || setweight(to_tsvector('simple', coalesce(lower(r.full_address), '')), 'C')
			    AS search_vector
			FROM restaurant r
			LEFT JOIN restaurant_food_category rfc ON rfc.restaurant_id = r.id
			LEFT JOIN food_category fc ON fc.id = rfc.food_category_id
			GROUP BY r.id, r.name, r.full_address, r.location, r.updated_at, r.deleted_at
			""");
	}

	@AfterEach
	void restoreStrategy() {
		searchQueryProperties.setStrategy(originalStrategy);
	}

	// ────────────────────────────────────────────────────────────
	// [1] 스펙/계약 테스트
	// ────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("[1] 검색 스펙 계약 테스트")
	class SpecTests {

		@BeforeEach
		void useDefaultStrategy() {
			searchQueryProperties.setStrategy(SearchQueryStrategy.ONE_STEP);
		}

		@Test
		@DisplayName("이름 조건 + 거리 조건 + 점수 정렬이 요구조건과 일치한다")
		void searchRestaurantsMatchesSpec() {
			Restaurant exact = createRestaurant("치킨", point(LON, LAT));
			Restaurant similar = createRestaurant("치킨킹", point(LON + 0.002, LAT + 0.002));
			Restaurant other = createRestaurant("피자", point(LON, LAT));
			Restaurant outside = createRestaurant("치킨먼곳", point(LON + 0.5, LAT + 0.5));
			Restaurant deleted = createRestaurant("치킨삭제", point(LON, LAT));
			deleted.softDelete(Instant.parse("2000-01-01T00:00:00Z"));

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
				"치킨", null, 20, LAT, LON, RADIUS_METERS);

			List<Long> actualIds = ids(result);

			assertThat(actualIds).containsExactlyElementsOf(expectedIds);
			assertThat(actualIds).doesNotContain(other.getId(), outside.getId(), deleted.getId());
		}

		@Test
		@DisplayName("soft delete된 식당은 결과에 포함되지 않는다")
		void softDeletedIsExcluded() {
			Restaurant active = createRestaurant("치킨", point(LON, LAT));
			Restaurant deleted = createRestaurant("치킨삭제", point(LON, LAT));
			restaurantRepository.saveAll(List.of(active, deleted));
			restaurantRepository.flush();
			deleted.softDelete(Instant.now());
			restaurantRepository.saveAndFlush(deleted);

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 20, LAT, LON, RADIUS_METERS);

			assertThat(ids(result)).containsOnly(active.getId());
		}

		@Test
		@DisplayName("반경 밖 식당은 결과에 포함되지 않는다")
		void outsideRadiusIsExcluded() {
			Restaurant inside = createRestaurant("치킨", point(LON, LAT));
			Restaurant outside = createRestaurant("치킨멀리", point(LON + 0.5, LAT + 0.5));
			restaurantRepository.saveAll(List.of(inside, outside));
			restaurantRepository.flush();

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 20, LAT, LON, RADIUS_METERS);

			assertThat(ids(result)).containsOnly(inside.getId());
			assertThat(ids(result)).doesNotContain(outside.getId());
		}

		@Test
		@DisplayName("완전 일치 식당이 유사 일치 식당보다 높은 점수로 먼저 반환된다")
		void exactMatchRanksHigherThanSimilar() {
			Restaurant exact = createRestaurant("치킨", point(LON, LAT));
			Restaurant similar = createRestaurant("치킨킹", point(LON, LAT));
			restaurantRepository.saveAll(List.of(similar, exact));
			restaurantRepository.flush();

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 20, LAT, LON, RADIUS_METERS);

			assertThat(ids(result).get(0)).isEqualTo(exact.getId());
		}

		@Test
		@DisplayName("키워드와 무관한 식당은 반환되지 않는다")
		void unrelatedKeywordExcluded() {
			Restaurant unrelated = createRestaurant("피자", point(LON, LAT));
			restaurantRepository.saveAndFlush(unrelated);

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 20, LAT, LON, RADIUS_METERS);

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("매칭 결과가 없으면 빈 리스트를 반환한다")
		void noMatchReturnsEmptyList() {
			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"없는키워드xyz", null, 20, LAT, LON, RADIUS_METERS);

			assertThat(result).isEmpty();
		}

		@Test
		@DisplayName("위치 정보가 없으면 거리 필터 없이 전체 범위 검색한다")
		void nullLocationSkipsGeoFilter() {
			Restaurant nearby = createRestaurant("치킨", point(LON, LAT));
			Restaurant farAway = createRestaurant("치킨멀리", point(LON + 5.0, LAT + 5.0));
			restaurantRepository.saveAll(List.of(nearby, farAway));
			restaurantRepository.flush();

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 20, null, null, null);

			assertThat(ids(result)).containsExactlyInAnyOrder(nearby.getId(), farAway.getId());
		}
	}

	// ────────────────────────────────────────────────────────────
	// [2] NULL 커서 회귀 테스트 — PostgreSQL 42P18 방지
	// ────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("[2] NULL 커서 회귀 테스트 (42P18 방지)")
	class NullCursorRegressionTests {

		static Stream<SearchQueryStrategy> nativeStrategies() {
			return Stream.of(
				SearchQueryStrategy.FTS_MV_RANKED,
				SearchQueryStrategy.HYBRID_SPLIT_CANDIDATES,
				SearchQueryStrategy.GEO_FIRST_HYBRID,
				SearchQueryStrategy.READ_MODEL_TWO_STEP,
				SearchQueryStrategy.MV_SINGLE_PASS);
		}

		static Stream<SearchQueryStrategy> allStrategies() {
			return Stream.of(SearchQueryStrategy.values());
		}

		@ParameterizedTest(name = "전략={0}, 위치 있음")
		@MethodSource("nativeStrategies")
		@DisplayName("native 전략에서 cursor=null + 위치 있음 → 500 에러 없음")
		void nullCursorWithLocation_nativeStrategies(SearchQueryStrategy strategy) {
			searchQueryProperties.setStrategy(strategy);
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMvIfNeeded(strategy);

			assertThatCode(() -> searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS))
				.doesNotThrowAnyException();
		}

		@ParameterizedTest(name = "전략={0}, 위치 없음")
		@MethodSource("nativeStrategies")
		@DisplayName("native 전략에서 cursor=null + 위치 없음 → 500 에러 없음")
		void nullCursorWithoutLocation_nativeStrategies(SearchQueryStrategy strategy) {
			if (strategy == SearchQueryStrategy.GEO_FIRST_HYBRID) {
				// GEO_FIRST_HYBRID는 위치 없으면 HYBRID_SPLIT_CANDIDATES로 위임하므로 별도 검증 불필요
				return;
			}
			searchQueryProperties.setStrategy(strategy);
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMvIfNeeded(strategy);

			assertThatCode(() -> searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, null, null, null))
				.doesNotThrowAnyException();
		}

		@ParameterizedTest(name = "전략={0}")
		@MethodSource("allStrategies")
		@DisplayName("모든 전략에서 cursor=null 첫 페이지 → 예외 없음")
		void nullCursor_allStrategies(SearchQueryStrategy strategy) {
			searchQueryProperties.setStrategy(strategy);
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMvIfNeeded(strategy);

			assertThatCode(() -> searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS))
				.doesNotThrowAnyException();
		}
	}

	// ────────────────────────────────────────────────────────────
	// [3] 커서 페이지네이션 테스트
	// ────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("[3] 커서 페이지네이션 테스트")
	class CursorPaginationTests {

		@BeforeEach
		void useOneStep() {
			searchQueryProperties.setStrategy(SearchQueryStrategy.ONE_STEP);
		}

		@Test
		@DisplayName("2페이지 결과에 1페이지 항목이 중복되지 않는다")
		void secondPageHasNoDuplicates() {
			saveRestaurants("치킨", 5);

			List<SearchRestaurantCursorRow> page1 = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 3, LAT, LON, RADIUS_METERS);
			SearchCursor cursor = cursorFrom(page1.get(page1.size() - 1));

			List<SearchRestaurantCursorRow> page2 = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", cursor, 3, LAT, LON, RADIUS_METERS);

			assertThat(ids(page1)).doesNotContainAnyElementsOf(ids(page2));
		}

		@Test
		@DisplayName("두 페이지를 합치면 전체 결과와 일치한다")
		void twoPagesCoverAllResults() {
			saveRestaurants("치킨", 5);

			List<SearchRestaurantCursorRow> all = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS);

			List<SearchRestaurantCursorRow> page1 = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 3, LAT, LON, RADIUS_METERS);
			SearchCursor cursor = cursorFrom(page1.get(page1.size() - 1));
			List<SearchRestaurantCursorRow> page2 = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", cursor, 10, LAT, LON, RADIUS_METERS);

			List<Long> combined = Stream.concat(ids(page1).stream(), ids(page2).stream()).toList();
			assertThat(combined).containsExactlyElementsOf(ids(all));
		}

		@Test
		@DisplayName("마지막 항목 이후 커서로 조회하면 빈 리스트를 반환한다")
		void afterLastItemReturnsEmpty() {
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));

			List<SearchRestaurantCursorRow> page1 = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS);
			SearchCursor cursor = cursorFrom(page1.get(page1.size() - 1));

			List<SearchRestaurantCursorRow> page2 = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", cursor, 10, LAT, LON, RADIUS_METERS);

			assertThat(page2).isEmpty();
		}

		@Test
		@DisplayName("동일 커서로 같은 페이지를 두 번 조회하면 동일한 결과를 반환한다")
		void firstPageIsIdempotent() {
			saveRestaurants("치킨", 3);

			List<Long> first = ids(searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS));
			List<Long> second = ids(searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS));

			assertThat(first).containsExactlyElementsOf(second);
		}

		@Test
		@DisplayName("2페이지 조회 사이에 삭제된 식당은 2페이지 결과에 포함되지 않는다")
		void deletedExcludedOnSecondPage() {
			saveRestaurants("치킨", 4);

			List<SearchRestaurantCursorRow> page1 = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 2, LAT, LON, RADIUS_METERS);

			// 2페이지 첫 번째 예정 항목을 삭제
			List<SearchRestaurantCursorRow> peek = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 3, LAT, LON, RADIUS_METERS);
			if (peek.size() > 2) {
				Long toDeleteId = peek.get(2).restaurant().id();
				Restaurant toDelete = restaurantRepository.findById(toDeleteId).orElseThrow();
				toDelete.softDelete(Instant.now());
				restaurantRepository.saveAndFlush(toDelete);
			}

			SearchCursor cursor = cursorFrom(page1.get(page1.size() - 1));
			List<SearchRestaurantCursorRow> page2 = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", cursor, 10, LAT, LON, RADIUS_METERS);

			Long deletedId = peek.size() > 2 ? peek.get(2).restaurant().id() : null;
			if (deletedId != null) {
				List<Long> page2Ids = ids(page2);
				assertThat(page2Ids).doesNotContain(deletedId);
			}
		}
	}

	// ────────────────────────────────────────────────────────────
	// [4] FTS_MV_RANKED 전략 특화 테스트
	// ────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("[4] FTS_MV_RANKED 전략 테스트")
	class FtsMvRankedTests {

		@BeforeEach
		void useFtsMvRanked() {
			searchQueryProperties.setStrategy(SearchQueryStrategy.FTS_MV_RANKED);
		}

		@Test
		@DisplayName("cursor=null, 위치 있음 → 500 에러 없음 (42P18 회귀)")
		void nullCursorWithLocation_noException() {
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMv();

			assertThatCode(() -> searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("cursor=null, 위치 없음 → 500 에러 없음 (42P18 회귀)")
		void nullCursorWithoutLocation_noException() {
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMv();

			assertThatCode(() -> searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, null, null, null))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("FTS_MV_RANKED 결과의 ftsRank 필드는 null이 아니다")
		void ftsRankIsNotNull() {
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMv();

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS);

			assertThat(result).isNotEmpty();
			result.forEach(row -> assertThat(row.ftsRank()).isNotNull());
		}

		@Test
		@DisplayName("이름이 정확히 일치하는 식당의 nameExact 필드가 1이다")
		void nameExactFieldIsCorrect() {
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMv();

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS);

			assertThat(result).isNotEmpty();
			assertThat(result.get(0).nameExact()).isEqualTo(1);
		}

		@Test
		@DisplayName("위치 있음 → distanceMeters가 null이 아니다")
		void distanceMetersNonNullWhenLocationPresent() {
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMv();

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS);

			assertThat(result).isNotEmpty();
			result.forEach(row -> assertThat(row.distanceMeters()).isNotNull());
		}

		@Test
		@DisplayName("위치 없음 → distanceMeters가 null이다")
		void distanceMetersNullWhenLocationAbsent() {
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMv();

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, null, null, null);

			assertThat(result).isNotEmpty();
			result.forEach(row -> assertThat(row.distanceMeters()).isNull());
		}
	}

	// ────────────────────────────────────────────────────────────
	// [5] MV_SINGLE_PASS 전략 특화 테스트
	// ────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("[5] MV_SINGLE_PASS 전략 테스트")
	class MvSinglePassTests {

		@BeforeEach
		void useMvSinglePass() {
			searchQueryProperties.setStrategy(SearchQueryStrategy.MV_SINGLE_PASS);
		}

		@Test
		@DisplayName("cursor=null → 500 에러 없음 (42P18 회귀)")
		void nullCursor_noException() {
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMv();

			assertThatCode(() -> searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS))
				.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("MV_SINGLE_PASS 결과의 ftsRank 필드는 null이다 (FTS 컬럼 없음)")
		void ftsRankIsNull() {
			restaurantRepository.saveAndFlush(createRestaurant("치킨", point(LON, LAT)));
			refreshMv();

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 10, LAT, LON, RADIUS_METERS);

			assertThat(result).isNotEmpty();
			result.forEach(row -> assertThat(row.ftsRank()).isNull());
		}
	}

	// ────────────────────────────────────────────────────────────
	// [6] 크기 경계 테스트
	// ────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("[6] 크기 경계 테스트")
	class SizeBoundaryTests {

		@BeforeEach
		void useOneStep() {
			searchQueryProperties.setStrategy(SearchQueryStrategy.ONE_STEP);
		}

		@Test
		@DisplayName("size=1이면 최대 1건만 반환한다")
		void sizeOneReturnsAtMostOne() {
			saveRestaurants("치킨", 5);

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 1, LAT, LON, RADIUS_METERS);

			assertThat(result).hasSize(1);
		}

		@Test
		@DisplayName("size가 데이터 수보다 크면 있는 데이터만 반환한다")
		void sizeExceedsDataReturnsAll() {
			saveRestaurants("치킨", 3);

			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				"치킨", null, 100, LAT, LON, RADIUS_METERS);

			assertThat(result).hasSize(3);
		}
	}

	// ────────────────────────────────────────────────────────────
	// helpers
	// ────────────────────────────────────────────────────────────

	private List<Long> ids(List<SearchRestaurantCursorRow> rows) {
		return rows.stream()
			.map(row -> row.restaurant().id())
			.collect(Collectors.toList());
	}

	private SearchCursor cursorFrom(SearchRestaurantCursorRow row) {
		return new SearchCursor(
			row.nameExact(),
			row.nameSimilarity(),
			row.ftsRank(),
			row.distanceMeters(),
			row.categoryMatch(),
			row.addressMatch(),
			row.restaurant().updatedAt(),
			row.restaurant().id());
	}

	private void saveRestaurants(String baseName, int count) {
		List<Restaurant> restaurants = Stream.iterate(0, i -> i + 1)
			.limit(count)
			.map(i -> createRestaurant(baseName + i, point(LON + i * 0.001, LAT + i * 0.001)))
			.toList();
		restaurantRepository.saveAll(restaurants);
		restaurantRepository.flush();
	}

	private void refreshMv() {
		jdbcTemplate.execute("REFRESH MATERIALIZED VIEW restaurant_search_mv");
	}

	private void refreshMvIfNeeded(SearchQueryStrategy strategy) {
		if (strategy == SearchQueryStrategy.FTS_MV_RANKED
			|| strategy == SearchQueryStrategy.READ_MODEL_TWO_STEP
			|| strategy == SearchQueryStrategy.MV_SINGLE_PASS) {
			refreshMv();
		}
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
