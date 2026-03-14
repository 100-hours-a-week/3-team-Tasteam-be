package com.tasteam.domain.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.file.dto.response.DomainImageItem;
import com.tasteam.domain.file.entity.DomainType;
import com.tasteam.domain.file.service.FileService;
import com.tasteam.domain.group.repository.GroupMemberRepository;
import com.tasteam.domain.main.dto.request.MainPageRequest;
import com.tasteam.domain.main.dto.response.AiRecommendResponse;
import com.tasteam.domain.promotion.service.PromotionService;
import com.tasteam.domain.restaurant.entity.RestaurantReviewSummary;
import com.tasteam.domain.restaurant.repository.RestaurantFoodCategoryRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSummaryRepository;
import com.tasteam.domain.restaurant.repository.projection.MainRestaurantDistanceProjection;
import com.tasteam.domain.restaurant.repository.projection.RestaurantCategoryProjection;

@UnitTest
@DisplayName("[유닛](Main) MainService 단위 테스트")
class MainServiceTest {

	private static final Instant NOW = Instant.parse("2026-03-13T00:00:00Z");

	@Test
	@DisplayName("AI 추천 조회 시 legacy summary 키를 reviewSummary로 노출한다")
	void getAiRecommend_usesLegacySummaryKey() {
		// given
		MainDataService mainDataService = mock(MainDataService.class);
		RestaurantFoodCategoryRepository categoryRepository = mock(RestaurantFoodCategoryRepository.class);
		RestaurantReviewSummaryRepository summaryRepository = mock(RestaurantReviewSummaryRepository.class);
		FileService fileService = mock(FileService.class);
		Executor executor = Runnable::run;

		MainRestaurantDistanceProjection restaurant = projection(1L, "레거시 맛집");
		when(mainDataService.fetchAiSectionAll()).thenReturn(List.of(restaurant));

		RestaurantCategoryProjection category = mock(RestaurantCategoryProjection.class);
		when(category.getRestaurantId()).thenReturn(1L);
		when(category.getCategoryName()).thenReturn("한식");
		when(categoryRepository.findCategoriesByRestaurantIds(List.of(1L))).thenReturn(List.of(category));

		when(summaryRepository.findByRestaurantIdIn(List.of(1L))).thenReturn(List.of(
			RestaurantReviewSummary.create(1L, 0L, "dummy-v1", Map.of("summary", "레거시 AI 요약"), NOW)));

		when(fileService.getDomainImageUrls(eq(DomainType.RESTAURANT), eq(List.of(1L))))
			.thenReturn(Map.of(1L, List.of(new DomainImageItem(11L, "https://cdn.example.com/legacy.webp"))));

		MainService service = new MainService(
			mainDataService,
			categoryRepository,
			summaryRepository,
			mock(GroupMemberRepository.class),
			fileService,
			mock(PromotionService.class),
			executor);

		// when
		AiRecommendResponse response = service.getAiRecommend(null, new MainPageRequest(null, null));

		// then
		assertThat(response.section().items()).hasSize(1);
		assertThat(response.section().items().getFirst().restaurantId()).isEqualTo(1L);
		assertThat(response.section().items().getFirst().reviewSummary()).isEqualTo("레거시 AI 요약");
	}

	private MainRestaurantDistanceProjection projection(Long id, String name) {
		MainRestaurantDistanceProjection projection = mock(MainRestaurantDistanceProjection.class);
		when(projection.getId()).thenReturn(id);
		when(projection.getName()).thenReturn(name);
		when(projection.getDistanceMeter()).thenReturn(null);
		return projection;
	}
}
