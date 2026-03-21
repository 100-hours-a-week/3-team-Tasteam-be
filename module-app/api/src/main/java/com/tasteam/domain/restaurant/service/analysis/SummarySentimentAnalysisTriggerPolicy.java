package com.tasteam.domain.restaurant.service.analysis;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SummarySentimentAnalysisTriggerPolicy {

	private final RestaurantReviewAnalysisPolicyProperties properties;

	public boolean shouldRun(int reviewCount) {
		return reviewCount >= properties.getSummaryBatchSize()
			&& reviewCount % properties.getSummaryBatchSize() == 0;
	}
}
