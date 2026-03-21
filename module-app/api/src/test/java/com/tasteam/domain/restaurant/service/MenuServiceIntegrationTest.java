package com.tasteam.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.tasteam.domain.restaurant.entity.Menu;
import com.tasteam.domain.restaurant.entity.MenuCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.MenuCategoryRepository;
import com.tasteam.domain.restaurant.repository.MenuRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;

@ServiceIntegrationTest
@Transactional
@DisplayName("[통합](Menu) MenuService 통합 테스트")
class MenuServiceIntegrationTest {

	@Autowired
	private MenuService menuService;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private MenuCategoryRepository menuCategoryRepository;

	@Autowired
	private MenuRepository menuRepository;

	@Nested
	@DisplayName("메뉴 조회")
	class GetRestaurantMenus {

		@Test
		@DisplayName("카테고리 옵션에 따라 메뉴 목록이 구성된다")
		void getRestaurantMenusSuccess() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("조회 음식점"));
			MenuCategory category = menuCategoryRepository.save(MenuCategory.create(restaurant, "메인", 0));
			menuRepository.save(Menu.create(category, "김치찌개", "매콤한 찌개", 9000, null, true, 0));

			RestaurantMenuResponse response = menuService.getRestaurantMenus(
				restaurant.getId(), false, false);

			assertThat(response.categories()).hasSize(1);
			assertThat(response.categories().getFirst().menus()).hasSize(1);
		}

		@Test
		@DisplayName("존재하지 않는 음식점이면 실패한다")
		void getRestaurantMenusNotFoundFails() {
			assertThatThrownBy(() -> menuService.getRestaurantMenus(999999L, false, false))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(RestaurantErrorCode.RESTAURANT_NOT_FOUND.name());
		}
	}

	private Restaurant createRestaurant(String name) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			name,
			"서울특별시 강남구 테헤란로 123",
			geometryFactory.createPoint(new Coordinate(127.0, 37.5)),
			"02-1111-2222");
	}
}
