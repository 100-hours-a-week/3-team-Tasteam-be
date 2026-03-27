package com.tasteam.domain.analytics.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.restaurant.entity.FoodCategory;
import com.tasteam.domain.restaurant.entity.Menu;
import com.tasteam.domain.restaurant.entity.MenuCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.entity.RestaurantAddress;
import com.tasteam.domain.restaurant.entity.RestaurantFoodCategory;
import com.tasteam.domain.restaurant.repository.FoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.MenuCategoryRepository;
import com.tasteam.domain.restaurant.repository.MenuRepository;
import com.tasteam.domain.restaurant.repository.RestaurantAddressRepository;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;
import com.tasteam.infra.storage.StorageClient;

@ServiceIntegrationTest
@Transactional
@DisplayName("[통합](Analytics) RawDataExportService 통합 테스트")
class RawDataExportServiceIntegrationTest {

	@Autowired
	private RawDataExportService rawDataExportService;

	@Autowired
	private StorageClient storageClient;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private RestaurantAddressRepository restaurantAddressRepository;

	@Autowired
	private FoodCategoryRepository foodCategoryRepository;

	@Autowired
	private RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;

	@Autowired
	private MenuCategoryRepository menuCategoryRepository;

	@Autowired
	private MenuRepository menuRepository;

	private static final LocalDate TEST_DT = LocalDate.of(2026, 3, 11);

	@BeforeEach
	void clearStoragePrefix() {
		deleteAllByPrefix("raw/restaurants/dt=" + TEST_DT + "/");
		deleteAllByPrefix("raw/menus/dt=" + TEST_DT + "/");
	}

	@Test
	@DisplayName("restaurants/menus CSV 헤더와 행 값이 계약 스키마로 적재된다")
	void export_writesContractSchemaRows() {
		Restaurant restaurant = restaurantRepository.save(createRestaurant("테스트식당"));
		restaurantAddressRepository.save(RestaurantAddress.create(
			restaurant,
			"경기도",
			"성남시 분당구",
			"삼평동",
			"13487"));
		FoodCategory foodCategory = foodCategoryRepository.save(FoodCategory.create("한식"));
		restaurantFoodCategoryRepository.save(RestaurantFoodCategory.create(restaurant, foodCategory));
		MenuCategory menuCategory = menuCategoryRepository.save(MenuCategory.create(restaurant, "메인", 0));
		menuRepository.save(Menu.create(menuCategory, "추천메뉴", "", 12000, null, true, 1));
		menuRepository.save(Menu.create(menuCategory, "일반메뉴", "", 10000, null, false, 0));

		rawDataExportService.export(new RawDataExportCommand(
			TEST_DT,
			EnumSet.of(RawDataType.RESTAURANTS, RawDataType.MENUS),
			"it-1"));

		String restaurantPrefix = "raw/restaurants/dt=2026-03-11/";
		String menuPrefix = "raw/menus/dt=2026-03-11/";
		String restaurantCsv = new String(
			storageClient.downloadObject(restaurantPrefix + "part-00001.csv"),
			StandardCharsets.UTF_8);
		String menuCsv = new String(
			storageClient.downloadObject(menuPrefix + "part-00001.csv"),
			StandardCharsets.UTF_8);

		assertThat(storageClient.listObjects(restaurantPrefix))
			.contains(restaurantPrefix + "part-00001.csv", restaurantPrefix + "_SUCCESS");
		assertThat(storageClient.listObjects(menuPrefix))
			.contains(menuPrefix + "part-00001.csv", menuPrefix + "_SUCCESS");

		assertThat(firstLine(restaurantCsv)).isEqualTo(
			"restaurant_id,restaurant_name,sido,sigungu,eupmyeondong,geohash,food_category_id,food_category_name");
		assertThat(restaurantCsv).contains(
			restaurant.getId() + ",테스트식당,경기도,성남시 분당구,삼평동,",
			"," + foodCategory.getId() + ",한식");

		assertThat(firstLine(menuCsv)).isEqualTo(
			"restaurant_id,menu_count,price_min,price_max,price_mean,price_median,representative_menu_name,top_menus,price_tier");
		assertThat(menuCsv).contains(
			restaurant.getId() + ",2,10000,12000,11000.00,11000.00,추천메뉴,",
			"추천메뉴",
			"일반메뉴",
			"UNDER_20000");
	}

	private void deleteAllByPrefix(String prefix) {
		List<String> keys = storageClient.listObjects(prefix);
		for (String key : keys) {
			storageClient.deleteObject(key);
		}
	}

	private String firstLine(String csv) {
		return csv.split("\\r?\\n", -1)[0];
	}

	private Restaurant createRestaurant(String name) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			name,
			"경기도 성남시 분당구 판교역로 235",
			geometryFactory.createPoint(new Coordinate(127.1126, 37.3952)),
			"031-0000-0000");
	}
}
