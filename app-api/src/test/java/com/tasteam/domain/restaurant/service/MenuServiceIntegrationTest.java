package com.tasteam.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.restaurant.dto.request.MenuBulkCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCategoryCreateRequest;
import com.tasteam.domain.restaurant.dto.request.MenuCreateRequest;
import com.tasteam.domain.restaurant.dto.response.RestaurantMenuResponse;
import com.tasteam.domain.restaurant.entity.MenuCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.repository.MenuCategoryRepository;
import com.tasteam.domain.restaurant.repository.MenuRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.RestaurantErrorCode;

@ServiceIntegrationTest
@Transactional
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
	@DisplayName("메뉴 카테고리 생성")
	class CreateMenuCategory {

		@Test
		@DisplayName("음식점에 카테고리가 생성된다")
		void createMenuCategorySuccess() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("카테고리 음식점"));
			Long categoryId = menuService.createMenuCategory(
				restaurant.getId(),
				new MenuCategoryCreateRequest("메인메뉴", 0));

			MenuCategory category = menuCategoryRepository.findById(categoryId).orElseThrow();
			assertThat(category.getName()).isEqualTo("메인메뉴");
		}

		@Test
		@DisplayName("존재하지 않는 음식점이면 실패한다")
		void createMenuCategoryNotFoundFails() {
			assertThatThrownBy(() -> menuService.createMenuCategory(
				999999L,
				new MenuCategoryCreateRequest("메인메뉴", 0)))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(RestaurantErrorCode.RESTAURANT_NOT_FOUND.name());
		}
	}

	@Nested
	@DisplayName("메뉴 생성")
	class CreateMenu {

		@Test
		@DisplayName("음식점-카테고리 유효성 검증 후 메뉴가 저장된다")
		void createMenuSuccess() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("메뉴 음식점"));
			MenuCategory category = menuCategoryRepository.save(MenuCategory.create(
				restaurant, "메인", 0));

			Long menuId = menuService.createMenu(
				restaurant.getId(),
				new MenuCreateRequest(category.getId(), "김치찌개", "매콤한 찌개", 9000, null, null, true, 0));

			assertThat(menuRepository.findById(menuId)).isPresent();
		}

		@Test
		@DisplayName("다른 음식점의 카테고리이면 실패한다")
		void createMenuWithOtherRestaurantCategoryFails() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("음식점 A"));
			Restaurant otherRestaurant = restaurantRepository.save(createRestaurant("음식점 B"));
			MenuCategory category = menuCategoryRepository.save(MenuCategory.create(
				otherRestaurant, "메인", 0));

			assertThatThrownBy(() -> menuService.createMenu(
				restaurant.getId(),
				new MenuCreateRequest(category.getId(), "된장찌개", "구수함", 8000, null, null, false, 0)))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(RestaurantErrorCode.MENU_CATEGORY_NOT_FOUND.name());
		}
	}

	@Nested
	@DisplayName("메뉴 일괄 생성")
	class CreateMenuBulk {

		@Test
		@DisplayName("메뉴가 일괄 저장되고 순서대로 적용된다")
		void createMenusBulkSuccess() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("일괄 음식점"));
			MenuCategory category = menuCategoryRepository.save(MenuCategory.create(
				restaurant, "메인", 0));

			List<Long> ids = menuService.createMenusBulk(
				restaurant.getId(),
				new MenuBulkCreateRequest(List.of(
					new MenuCreateRequest(category.getId(), "라면", "매운 라면", 7000, null, null, false, 0),
					new MenuCreateRequest(category.getId(), "우동", "담백한 우동", 8000, null, null, false, 1))));

			assertThat(ids).hasSize(2);
			assertThat(menuRepository.findAll()).hasSize(2);
		}

		@Test
		@DisplayName("카테고리 불일치가 있으면 실패한다")
		void createMenusBulkWithInvalidCategoryFails() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("일괄 실패 음식점"));
			Restaurant otherRestaurant = restaurantRepository.save(createRestaurant("다른 음식점"));
			MenuCategory category = menuCategoryRepository.save(MenuCategory.create(
				otherRestaurant, "메인", 0));

			assertThatThrownBy(() -> menuService.createMenusBulk(
				restaurant.getId(),
				new MenuBulkCreateRequest(List.of(
					new MenuCreateRequest(category.getId(), "라면", "매운 라면", 7000, null, null, false, 0)))))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(RestaurantErrorCode.MENU_CATEGORY_NOT_FOUND.name());
		}
	}

	@Nested
	@DisplayName("메뉴 조회")
	class GetRestaurantMenus {

		@Test
		@DisplayName("카테고리 옵션에 따라 메뉴 목록이 구성된다")
		void getRestaurantMenusSuccess() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("조회 음식점"));
			MenuCategory category = menuCategoryRepository.save(MenuCategory.create(
				restaurant, "메인", 0));
			menuService.createMenu(
				restaurant.getId(),
				new MenuCreateRequest(category.getId(), "김치찌개", "매콤한 찌개", 9000, null, null, true, 0));

			RestaurantMenuResponse response = menuService.getRestaurantMenus(
				restaurant.getId(), false, false);

			assertThat(response.categories()).hasSize(1);
			assertThat(response.categories().getFirst().menus()).hasSize(1);
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
