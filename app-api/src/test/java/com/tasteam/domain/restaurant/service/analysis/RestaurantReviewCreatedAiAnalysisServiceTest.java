package com.tasteam.domain.restaurant.service.analysis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.review.repository.ReviewRepository;
import com.tasteam.infra.ai.AiClient;
import com.tasteam.infra.ai.dto.AiSentimentAnalysisDisplayResponse;
import com.tasteam.infra.ai.dto.AiSummaryDisplayResponse;

@UnitTest
class RestaurantReviewCreatedAiAnalysisServiceTest {
	// TODO: runScheduledComparisonAnalysis 경로의 락 동작(중복 실행 방지/락 해제)을 검증하는 테스트 추가
	//  - 단일 인스턴스: 같은 restaurantId 중복 실행 방지
	//  - 다중 인스턴스 대비: 분산 락 도입 후 통합 테스트로 확장

	@Mock
	private AnalysisLock analysisLock;
	@Mock
	private SummarySentimentAnalysisTriggerPolicy summarySentimentTriggerPolicy;
	@Mock
	private ComparisonAnalysisTriggerPolicy comparisonTriggerPolicy;
	@Mock
	private RestaurantReviewAnalysisPolicyProperties policyProperties;
	@Mock
	private RestaurantReviewAnalysisStateService stateService;
	@Mock
	private RestaurantReviewAnalysisSnapshotService snapshotService;
	@Mock
	private AiClient aiClient;
	@Mock
	private ReviewRepository reviewRepository;

	@InjectMocks
	private RestaurantReviewAnalysisService service;

	@Test
	@DisplayName("락 획득에 실패하면 분석을 수행하지 않는다")
	void onReviewCreated_whenLockNotAcquired_skips() {
		long restaurantId = 10L;
		given(analysisLock.tryLock(restaurantId)).willReturn(false);

		service.onReviewCreated(restaurantId);

		verify(reviewRepository, never()).countByRestaurantIdAndDeletedAtIsNull(any());
		verify(analysisLock, never()).unlock(restaurantId);
	}

	@Test
	@DisplayName("Summary/Sentiment 조건만 만족하면 해당 분석만 수행한다")
	void onReviewCreated_runsOnlySummarySentiment() {
		long restaurantId = 10L;
		AiSummaryDisplayResponse summaryResponse = new AiSummaryDisplayResponse(
			restaurantId,
			"요약",
			Map.of("service", new AiSummaryDisplayResponse.CategorySummary("친절함", List.of(), List.of())));
		AiSentimentAnalysisDisplayResponse sentimentResponse = new AiSentimentAnalysisDisplayResponse(
			restaurantId,
			80,
			20);

		given(analysisLock.tryLock(restaurantId)).willReturn(true);
		given(reviewRepository.countByRestaurantIdAndDeletedAtIsNull(restaurantId)).willReturn(5L);
		given(summarySentimentTriggerPolicy.shouldRun(5)).willReturn(true);
		given(policyProperties.getSummaryBatchSize()).willReturn(10);
		given(reviewRepository.findByRestaurantIdAndDeletedAtIsNull(eq(restaurantId), any())).willReturn(List.of());
		given(aiClient.summarize(any())).willReturn(summaryResponse);
		given(aiClient.analyzeSentiment(any())).willReturn(sentimentResponse);

		service.onReviewCreated(restaurantId);

		verify(aiClient).summarize(any());
		verify(aiClient).analyzeSentiment(any());
		verify(aiClient, never()).extractStrengths(any());
		verify(stateService).markAnalyzingForSummary(restaurantId);
		verify(snapshotService).saveSummaryAndSentiment(eq(restaurantId), any(), any(), any(), any(), any());
		verify(analysisLock).unlock(restaurantId);
	}

	@Test
	@DisplayName("분석 중 예외가 발생해도 락은 해제된다")
	void onReviewCreated_unlocksOnFailure() {
		long restaurantId = 10L;
		given(analysisLock.tryLock(restaurantId)).willReturn(true);
		given(reviewRepository.countByRestaurantIdAndDeletedAtIsNull(restaurantId))
			.willThrow(new RuntimeException("boom"));

		service.onReviewCreated(restaurantId);

		verify(analysisLock).unlock(restaurantId);
	}
}
