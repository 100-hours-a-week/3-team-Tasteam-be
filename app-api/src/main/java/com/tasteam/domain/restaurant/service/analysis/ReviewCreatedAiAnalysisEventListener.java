package com.tasteam.domain.restaurant.service.analysis;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.tasteam.domain.review.event.ReviewCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReviewCreatedAiAnalysisEventListener {

	private final RestaurantReviewAnalysisService restaurantReviewAnalysisService;

	@EventListener
	public void onReviewCreated(ReviewCreatedEvent event) {
		restaurantReviewAnalysisService.onReviewCreated(event.restaurantId());
	}
}
