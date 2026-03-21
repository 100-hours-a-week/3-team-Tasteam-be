package com.tasteam.domain.search.repository.executor;

/**
 * Native SQL 검색 전략에서 공통으로 사용하는 SQL 절(Fragment) 유틸리티.
 * <p>
 * 지리 필터, 거리 표현식, 거리 점수 절을 테이블 소스(MV / restaurant)별로 제공한다.
 * 각 Native Executor의 buildSql()에서 정적 메서드로 참조한다.
 */
public final class NativeSqlFragments {

	private NativeSqlFragments() {}

	/** restaurant_search_mv 기반 지리 필터 (ST_DWithin 조건, location_geo 직접 사용) */
	public static String geoFilter(boolean withLocation) {
		return withLocation
			? "AND ST_DWithin(mv.location_geo, geography(ST_MakePoint(:lng, :lat)), :radius_m)"
			: "";
	}

	/** restaurant 테이블 기반 지리 필터 */
	public static String geoFilterRestaurant(boolean withLocation) {
		return withLocation
			? "AND ST_DWithin(geography(r.location), geography(ST_MakePoint(:lng, :lat)), :radius_m)"
			: "";
	}

	/** restaurant_search_mv 기반 거리 표현식 (location_geo 직접 사용) */
	public static String distanceExprMv(boolean withLocation) {
		return withLocation
			? "ST_Distance(mv.location_geo, geography(ST_MakePoint(:lng, :lat)))"
			: "NULL::double precision ";
	}

	/** restaurant 테이블 기반 거리 표현식 */
	public static String distanceExprRestaurant(boolean withLocation) {
		return withLocation
			? "ST_Distance(geography(r.location), geography(ST_MakePoint(:lng, :lat)))"
			: "NULL::double precision ";
	}

	/** restaurant_search_mv 기반 거리 점수 SQL 절 (location_geo 직접 사용) */
	public static String distanceScoreMv(boolean withLocation) {
		return withLocation
			? "GREATEST(0.0, 1.0 - (ST_Distance(mv.location_geo, geography(ST_MakePoint(:lng, :lat))) / :radius_m)) * 50.0"
			: "0.0";
	}
}
