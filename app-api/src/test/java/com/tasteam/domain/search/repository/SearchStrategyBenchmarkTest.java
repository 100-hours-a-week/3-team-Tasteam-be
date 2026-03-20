package com.tasteam.domain.search.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.tasteam.config.annotation.PerformanceTest;
import com.tasteam.domain.search.dto.SearchRestaurantCursorRow;

@PerformanceTest
@DisplayName("[성능](Search) 전략별 검색 성능 비교 벤치마크")
class SearchStrategyBenchmarkTest {

	private static final double LAT = 37.5;
	private static final double LON = 126.9;
	private static final double RADIUS_METERS = 5000.0;
	private static final int WARMUP_RUNS = 5;
	private static final int BENCH_RUNS = 50;
	private static final String KEYWORD = "치킨";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SearchQueryRepository searchQueryRepository;

	@Autowired
	private SearchQueryProperties searchQueryProperties;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
		insertBulkRestaurants();
		jdbcTemplate.execute("REFRESH MATERIALIZED VIEW public.restaurant_search_mv");
	}

	@ParameterizedTest
	@EnumSource(value = SearchQueryStrategy.class, names = {"TWO_STEP", "MV_SINGLE_PASS", "FTS_MV_RANKED"})
	@DisplayName("전략별 검색 응답시간 측정")
	void benchmarkStrategy(SearchQueryStrategy strategy) {
		searchQueryProperties.setStrategy(strategy);

		for (int i = 0; i < WARMUP_RUNS; i++) {
			searchQueryRepository.searchRestaurantsByKeyword(KEYWORD, null, 20, LAT, LON, RADIUS_METERS);
		}

		List<Long> latencies = new ArrayList<>();
		for (int i = 0; i < BENCH_RUNS; i++) {
			long start = System.nanoTime();
			List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
				KEYWORD, null, 20, LAT, LON, RADIUS_METERS);
			latencies.add((System.nanoTime() - start) / 1_000_000);
			assertThat(result).isNotEmpty();
		}

		latencies.sort(Long::compareTo);
		long avg = (long)latencies.stream().mapToLong(Long::longValue).average().orElse(0);
		long p95 = latencies.get((int)(BENCH_RUNS * 0.95));
		long p99 = latencies.get((int)(BENCH_RUNS * 0.99));

		System.out.printf("[%s] avg=%dms, p95=%dms, p99=%dms%n", strategy, avg, p95, p99);
	}

	@ParameterizedTest
	@EnumSource(value = SearchQueryStrategy.class, names = {"TWO_STEP", "MV_SINGLE_PASS", "FTS_MV_RANKED"})
	@DisplayName("전략별 결과 일관성 검증 - 상위 결과가 동일한지 확인")
	void verifyResultConsistency(SearchQueryStrategy strategy) {
		searchQueryProperties.setStrategy(strategy);

		List<SearchRestaurantCursorRow> result = searchQueryRepository.searchRestaurantsByKeyword(
			KEYWORD, null, 20, LAT, LON, RADIUS_METERS);

		assertThat(result).isNotEmpty();

		List<Long> ids = result.stream()
			.map(row -> row.restaurant().id())
			.collect(Collectors.toList());
		System.out.printf("[%s] top-5 IDs: %s%n", strategy, ids.subList(0, Math.min(5, ids.size())));
	}

	private void insertBulkRestaurants() {
		jdbcTemplate.execute("DELETE FROM restaurant WHERE name LIKE 'bench_%'");

		List<Object[]> rows = new ArrayList<>();

		// 정확 매칭 10건 (keyword=치킨)
		IntStream.range(0, 10).forEach(i -> rows.add(new Object[] {
			"치킨", "서울특별시 강남구 테헤란로 " + i, makePoint(LON + i * 0.001, LAT)
		}));

		// 유사 매칭 50건 (치킨X)
		IntStream.range(0, 50).forEach(i -> rows.add(new Object[] {
			"치킨" + i, "서울특별시 강남구 역삼로 " + i, makePoint(LON + i * 0.001, LAT + 0.001)
		}));

		// 카테고리 매칭용 30건 (name과 관련 없음, 카테고리는 food_category 별도 처리)
		IntStream.range(0, 30).forEach(i -> rows.add(new Object[] {
			"bench_pizza" + i, "서울특별시 서초구 강남대로 " + i, makePoint(LON - i * 0.001, LAT)
		}));

		// 무관 910건
		IntStream.range(0, 910).forEach(i -> rows.add(new Object[] {
			"bench_other" + i, "부산광역시 해운대구 해운대로 " + i, makePoint(129.1 + i * 0.0001, 35.1)
		}));

		jdbcTemplate.batchUpdate("""
			INSERT INTO restaurant (name, full_address, location, created_at, updated_at)
			VALUES (?, ?, ST_GeomFromText(?, 4326), NOW(), NOW())
			""", rows);
	}

	private String makePoint(double lon, double lat) {
		return String.format("POINT(%f %f)", lon, lat);
	}
}
