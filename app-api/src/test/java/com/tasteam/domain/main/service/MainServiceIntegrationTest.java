package com.tasteam.domain.main.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.main.dto.request.MainPageRequest;
import com.tasteam.domain.main.dto.response.AiRecommendResponse;
import com.tasteam.domain.main.dto.response.HomePageResponse;
import com.tasteam.domain.main.dto.response.MainPageResponse;
import com.tasteam.domain.restaurant.entity.AiRestaurantReviewAnalysis;
import com.tasteam.domain.restaurant.entity.FoodCategory;
import com.tasteam.domain.restaurant.entity.Restaurant;
import com.tasteam.domain.restaurant.entity.RestaurantFoodCategory;
import com.tasteam.domain.restaurant.repository.AiRestaurantReviewAnalysisRepository;
import com.tasteam.domain.restaurant.repository.FoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantRepository;

@ServiceIntegrationTest
@Transactional
class MainServiceIntegrationTest {

	@Autowired
	private MainService mainService;

	@Autowired
	private RestaurantRepository restaurantRepository;

	@Autowired
	private FoodCategoryRepository foodCategoryRepository;

	@Autowired
	private RestaurantFoodCategoryRepository restaurantFoodCategoryRepository;

	@Autowired
	private AiRestaurantReviewAnalysisRepository aiRestaurantReviewAnalysisRepository;

	@Nested
	@DisplayName("메인 페이지 조회")
	class GetMainPage {

		@Test
		@DisplayName("위치 정보가 없으면 기본 섹션이 구성된다")
		void getMainWithoutLocation() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("메인 맛집"));
			FoodCategory category = foodCategoryRepository.save(FoodCategory.create("한식"));
			restaurantFoodCategoryRepository.save(RestaurantFoodCategory.create(restaurant, category));
			aiRestaurantReviewAnalysisRepository.save(
				AiRestaurantReviewAnalysis.create(restaurant.getId(), "AI 요약", BigDecimal.valueOf(0.75)));

			MainPageResponse response = mainService.getMain(null, new MainPageRequest(null, null));

			assertThat(response.sections()).hasSize(4);
			MainPageResponse.Section aiSection = response.sections().stream()
				.filter(section -> "AI_RECOMMEND".equals(section.type()))
				.findFirst()
				.orElseThrow();
			assertThat(aiSection.items()).isNotEmpty();
		}
	}

	@Nested
	@DisplayName("홈 페이지 조회")
	class GetHomePage {

		@Test
		@DisplayName("위치 정보가 없으면 홈 섹션이 구성된다")
		void getHomeWithoutLocation() {
			restaurantRepository.save(createRestaurant("홈 맛집"));

			HomePageResponse response = mainService.getHome(null, new MainPageRequest(null, null));

			assertThat(response.sections()).hasSize(2);
		}
	}

	@Nested
	@DisplayName("AI 추천 조회")
	class GetAiRecommend {

		@Test
		@DisplayName("AI 요약 데이터가 있으면 추천 섹션이 구성된다")
		void getAiRecommendWithoutLocation() {
			Restaurant restaurant = restaurantRepository.save(createRestaurant("AI 맛집"));
			aiRestaurantReviewAnalysisRepository.save(
				AiRestaurantReviewAnalysis.create(restaurant.getId(), "AI 요약", BigDecimal.valueOf(0.9)));

			AiRecommendResponse response = mainService.getAiRecommend(null, new MainPageRequest(null, null));

			assertThat(response.section().type()).isEqualTo("AI_RECOMMEND");
			assertThat(response.section().items()).isNotEmpty();
		}
	}

	private Restaurant createRestaurant(String name) {
		GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
		return Restaurant.create(
			name,
			"서울특별시 강남구 테헤란로 123",
			geometryFactory.createPoint(new Coordinate(127.0, 37.5)),
			"02-1234-5678");
	}
}
