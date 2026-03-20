package com.tasteam.domain.restaurant.service.analysis;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tasteam.domain.review.event.ReviewCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewCreatedAiAnalysisEventListener {

	private final RestaurantAnalysisFacade restaurantAnalysisFacade;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onReviewCreated(ReviewCreatedEvent event) {
		restaurantAnalysisFacade.onReviewCreated(event.restaurantId());
	}
}
