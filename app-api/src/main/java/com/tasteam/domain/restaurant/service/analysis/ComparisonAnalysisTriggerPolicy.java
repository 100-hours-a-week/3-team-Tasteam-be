package com.tasteam.domain.restaurant.service.analysis;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ComparisonAnalysisTriggerPolicy {

	private final RestaurantReviewAnalysisPolicyProperties properties;

	public boolean shouldRun(int reviewCount) {
		return reviewCount >= properties.getComparisonMinReviews()
			&& reviewCount % properties.getComparisonBatchSize() == 0;
	}
}
