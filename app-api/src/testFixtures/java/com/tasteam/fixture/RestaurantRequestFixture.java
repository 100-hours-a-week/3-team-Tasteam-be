package com.tasteam.fixture;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.tasteam.domain.restaurant.dto.request.MenuBulkCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCategoryCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCreateRequest;
import com.tasteam.domain.restaurant.dto.request.NearbyRestaurantQueryParams;
import com.tasteam.domain.restaurant.dto.request.RestaurantCreateRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantReviewListRequest;
import com.tasteam.domain.restaurant.dto.request.RestaurantUpdateRequest;
import com.tasteam.domain.restaurant.dto.request.WeeklyScheduleRequest;

public final class RestaurantRequestFixture {

	public static final Double DEFAULT_LAT = 37.5665;
	public static final Double DEFAULT_LNG = 126.9780;
	public static final Integer DEFAULT_RADIUS = 1000;
	public static final Integer DEFAULT_SIZE = 20;
	public static final String DEFAULT_NAME = "테스트식당";
	public static final String DEFAULT_ADDRESS = "서울시 강남구 테헤란로 1";

	private RestaurantRequestFixture() {}

	public static NearbyRestaurantQueryParams createNearbyParams() {
		return new NearbyRestaurantQueryParams(DEFAULT_LAT, DEFAULT_LNG, DEFAULT_RADIUS, null, null, DEFAULT_SIZE);
	}

	public static NearbyRestaurantQueryParams createNearbyParams(Double lat, Double longitude) {
		return new NearbyRestaurantQueryParams(lat, longitude, DEFAULT_RADIUS, null, null, DEFAULT_SIZE);
	}

	public static RestaurantCreateRequest createRestaurantRequest() {
		return new RestaurantCreateRequest(
			DEFAULT_NAME,
			DEFAULT_ADDRESS,
			List.of(1L, 2L),
			List.of(UUID.randomUUID()),
			List.of(new WeeklyScheduleRequest(1, LocalTime.of(9, 0), LocalTime.of(22, 0), false, null, null)));
	}

	public static RestaurantUpdateRequest createUpdateRequest() {
		return new RestaurantUpdateRequest(
			"수정된식당",
			List.of(1L),
			List.of(UUID.randomUUID()));
	}

	public static RestaurantReviewListRequest createReviewListRequest() {
		return new RestaurantReviewListRequest(null, DEFAULT_SIZE);
	}

	public static MenuCategoryCreateRequest createMenuCategoryRequest() {
		return new MenuCategoryCreateRequest("메인메뉴", 0);
	}

	public static MenuCreateRequest createMenuRequest() {
		return new MenuCreateRequest(1L, "된장찌개", "구수한 된장찌개", 9000, null, true, 0);
	}

	public static MenuBulkCreateRequest createMenuBulkRequest() {
		return new MenuBulkCreateRequest(List.of(
			new MenuCreateRequest(1L, "된장찌개", "구수한 된장찌개", 9000, null, true, 0),
			new MenuCreateRequest(1L, "김치찌개", "매콤한 김치찌개", 8000, null, false, 1)));
	}
}
