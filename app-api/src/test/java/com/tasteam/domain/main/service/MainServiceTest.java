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

@UnitTest
@DisplayName("[유닛](Main) MainService 단위 테스트")
class MainServiceTest {

	@Test
	@DisplayName("AI 추천 조회 시 summary가 reviewSummary로 노출된다")
	void getAiRecommend_returnsSummary() {
		// given
		MainDataService mainDataService = mock(MainDataService.class);
		MainMetadataLoader metadataLoader = mock(MainMetadataLoader.class);
		Executor executor = Runnable::run;

		MainRestaurantDistanceProjection restaurant = projection(1L, "맛집");
		when(mainDataService.fetchAiSectionAll()).thenReturn(List.of(restaurant));
		when(metadataLoader.loadCategories(List.of(1L))).thenReturn(Map.of(1L, List.of("한식")));
		when(metadataLoader.loadThumbnails(List.of(1L))).thenReturn(Map.of(1L, "https://cdn.example.com/img.webp"));
		when(metadataLoader.loadSummaries(List.of(1L))).thenReturn(Map.of(1L, "AI 요약 텍스트"));

		MainService service = new MainService(
			mainDataService,
			metadataLoader,
			mock(MainGroupRepository.class),
			mock(PromotionService.class),
			executor);

		// when
		AiRecommendResponse response = service.getAiRecommend(null, new MainPageRequest(null, null));

		// then
		assertThat(response.section().items()).hasSize(1);
		assertThat(response.section().items().getFirst().restaurantId()).isEqualTo(1L);
		assertThat(response.section().items().getFirst().reviewSummary()).isEqualTo("AI 요약 텍스트");
	}

	private MainRestaurantDistanceProjection projection(Long id, String name) {
		MainRestaurantDistanceProjection projection = mock(MainRestaurantDistanceProjection.class);
		when(projection.getId()).thenReturn(id);
		when(projection.getName()).thenReturn(name);
		when(projection.getDistanceMeter()).thenReturn(null);
		return projection;
	}
}
