package com.tasteam.domain.restaurant.service.analysis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.tasteam.batch.ai.review.runner.ReviewAnalysisRunner;
import com.tasteam.batch.ai.vector.runner.VectorUploadRunner;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.review.repository.ReviewRepository;

@UnitTest
class RestaurantReviewCreatedAiAnalysisServiceTest {

	@Mock
	private AnalysisLock analysisLock;
	@Mock
	private ComparisonAnalysisTriggerPolicy comparisonTriggerPolicy;
	@Mock
	private ReviewRepository reviewRepository;
	@Mock
	private VectorUploadRunner vectorUploadRunner;
	@Mock
	private ReviewAnalysisRunner reviewAnalysisRunner;

	@InjectMocks
	private RestaurantAnalysisFacade service;

	@Test
	@DisplayName("락 획득에 실패하면 리뷰 조회 없이 스킵하고 unlock 하지 않는다")
	void onReviewCreated_whenLockNotAcquired_skips() {
		long restaurantId = 10L;
		given(analysisLock.tryLock(restaurantId)).willReturn(false);

		service.onReviewCreated(restaurantId);

		verify(reviewRepository, never()).countByRestaurantIdAndDeletedAtIsNull(any());
		verify(analysisLock, never()).unlock(restaurantId);
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
