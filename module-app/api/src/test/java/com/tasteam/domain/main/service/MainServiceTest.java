package com.tasteam.domain.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.main.dto.request.MainPageRequest;
import com.tasteam.domain.main.dto.response.AiRecommendResponse;
import com.tasteam.domain.main.repository.MainGroupRepository;
import com.tasteam.domain.promotion.service.PromotionService;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;
import com.tasteam.domain.restaurant.service.FoodCategoryService;
import com.tasteam.fixture.MainPageResponseFixture;

@UnitTest
@DisplayName("[유닛](Main) MainService 단위 테스트")
class MainServiceTest {

	private final Executor syncExecutor = Runnable::run;

	@Test
	@DisplayName("AI 추천 조회 시 summary가 reviewSummary로 노출된다")
	void getAiRecommend_returnsSummary() {
		// given
		MainDataService mainDataService = mock(MainDataService.class);
		MainMetadataLoader metadataLoader = mock(MainMetadataLoader.class);

		Long restaurantId = MainPageResponseFixture.DEFAULT_RESTAURANT_ID;
		MainRestaurantDistanceProjection restaurant = projection(restaurantId,
			MainPageResponseFixture.DEFAULT_RESTAURANT_NAME);
		when(mainDataService.fetchAiSectionAll()).thenReturn(List.of(restaurant));
		when(metadataLoader.loadCategories(List.of(restaurantId)))
			.thenReturn(Map.of(restaurantId, MainPageResponseFixture.DEFAULT_CATEGORIES));
		when(metadataLoader.loadThumbnails(List.of(restaurantId)))
			.thenReturn(Map.of(restaurantId, MainPageResponseFixture.DEFAULT_THUMBNAIL));
		when(metadataLoader.loadSummaries(List.of(restaurantId)))
			.thenReturn(Map.of(restaurantId, MainPageResponseFixture.DEFAULT_SUMMARY));

		MainService service = new MainService(
			mainDataService,
			mock(MainRecommendationService.class),
			metadataLoader,
			mock(MainGroupRepository.class),
			mock(FoodCategoryService.class),
			mock(PromotionService.class),
			syncExecutor);

		// when
		AiRecommendResponse response = service.getAiRecommend(null, new MainPageRequest(null, null));

		// then
		assertThat(response.section().items()).hasSize(1);
		assertThat(response.section().items().getFirst().restaurantId()).isEqualTo(restaurantId);
		assertThat(response.section().items().getFirst().reviewSummary())
			.isEqualTo(MainPageResponseFixture.DEFAULT_SUMMARY);
	}

	private MainRestaurantDistanceProjection projection(Long id, String name) {
		MainRestaurantDistanceProjection projection = mock(MainRestaurantDistanceProjection.class);
		when(projection.getId()).thenReturn(id);
		when(projection.getName()).thenReturn(name);
		when(projection.getDistanceMeter()).thenReturn(null);
		return projection;
	}
}
