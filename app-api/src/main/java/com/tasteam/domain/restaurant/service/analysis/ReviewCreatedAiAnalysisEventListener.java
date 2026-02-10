package com.tasteam.domain.restaurant.service.analysis;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tasteam.domain.review.event.ReviewCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewCreatedAiAnalysisEventListener {

	private final RestaurantReviewAnalysisService restaurantReviewAnalysisService;

	@Async("aiAnalysisExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onReviewCreated(ReviewCreatedEvent event) {
		restaurantReviewAnalysisService.onReviewCreated(event.restaurantId());
	}
}
