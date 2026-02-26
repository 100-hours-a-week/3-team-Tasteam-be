package com.tasteam.domain.restaurant.service.analysis;

import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.review.event.ReviewCreatedEvent;

@UnitTest
@DisplayName("ReviewCreatedAiAnalysisEventListener")
class ReviewCreatedAiAnalysisEventListenerTest {

	@Mock
	private RestaurantAnalysisFacade restaurantAnalysisFacade;

	@InjectMocks
	private ReviewCreatedAiAnalysisEventListener reviewCreatedAiAnalysisEventListener;

	@Test
	@DisplayName("리뷰 이벤트 수신 시 AI 분석 Facade를 호출한다")
	void onReviewCreated_callsFacadeWithRestaurantId() {
		ReviewCreatedEvent event = new ReviewCreatedEvent(42L);

		reviewCreatedAiAnalysisEventListener.onReviewCreated(event);

		then(restaurantAnalysisFacade).should().onReviewCreated(42L);
	}
}
