package com.tasteam.domain.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.restaurant.dto.RestaurantAiDetails;
import com.tasteam.domain.restaurant.entity.RestaurantReviewSummary;
import com.tasteam.domain.restaurant.repository.RestaurantComparisonRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSentimentRepository;
import com.tasteam.domain.restaurant.repository.RestaurantReviewSummaryRepository;
import com.tasteam.domain.review.repository.ReviewRepository;

@UnitTest
@DisplayName("[유닛](Restaurant) RestaurantAiSummaryService 단위 테스트")
class RestaurantAiSummaryServiceTest {

	private static final Instant NOW = Instant.parse("2026-03-13T00:00:00Z");
	private static final String FALLBACK_SUMMARY = "리뷰가 더 모이면 AI 요약이 제공됩니다.";

	@Test
	@DisplayName("리뷰 요약 조회 시 legacy summary 키와 누락된 summary를 모두 안전하게 처리한다")
	void getReviewSummariesWithFallback_handlesLegacyAndMissingSummaryKeys() {
		// given
		RestaurantReviewSummaryRepository summaryRepository = mock(RestaurantReviewSummaryRepository.class);
		when(summaryRepository.findByRestaurantIdIn(List.of(1L, 2L, 3L, 4L))).thenReturn(List.of(
			createSummary(1L, Map.of("summary", "레거시 요약")),
			createSummary(2L, Map.of("overall_summary", "최신 요약")),
			createSummary(3L, Map.of("highlights", List.of("맛있다")))));
		RestaurantAiSummaryService service = new RestaurantAiSummaryService(
			mock(RestaurantComparisonRepository.class),
			summaryRepository,
			mock(RestaurantReviewSentimentRepository.class),
			mock(ReviewRepository.class));

		// when
		Map<Long, String> result = service.getReviewSummariesWithFallback(List.of(1L, 2L, 3L, 4L));

		// then
		assertThat(result)
			.containsEntry(1L, "레거시 요약")
			.containsEntry(2L, "최신 요약")
			.containsEntry(3L, FALLBACK_SUMMARY)
			.containsEntry(4L, FALLBACK_SUMMARY);
	}

	@Test
	@DisplayName("음식점 AI 상세 조회 시 legacy summary 키를 overall summary로 사용한다")
	void getRestaurantAiDetails_usesLegacySummaryKey() {
		// given
		RestaurantReviewSummaryRepository summaryRepository = mock(RestaurantReviewSummaryRepository.class);
		when(summaryRepository.findByRestaurantId(1L)).thenReturn(Optional.of(
			createSummary(1L, Map.of("summary", "레거시 요약"))));

		RestaurantReviewSentimentRepository sentimentRepository = mock(RestaurantReviewSentimentRepository.class);
		when(sentimentRepository.findByRestaurantId(1L)).thenReturn(Optional.empty());

		RestaurantComparisonRepository comparisonRepository = mock(RestaurantComparisonRepository.class);
		when(comparisonRepository.findByRestaurantId(1L)).thenReturn(Optional.empty());

		RestaurantAiSummaryService service = new RestaurantAiSummaryService(
			comparisonRepository,
			summaryRepository,
			sentimentRepository,
			mock(ReviewRepository.class));

		// when
		RestaurantAiDetails result = service.getRestaurantAiDetails(1L);

		// then
		assertThat(result.summary()).isNotNull();
		assertThat(result.summary().overallSummary()).isEqualTo("레거시 요약");
		assertThat(result.summary().categoryDetails()).isEmpty();
	}

	private RestaurantReviewSummary createSummary(Long restaurantId, Map<String, Object> summaryJson) {
		return RestaurantReviewSummary.create(restaurantId, 0L, "dummy-v1", summaryJson, NOW);
	}
}
