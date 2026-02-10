package com.tasteam.domain.restaurant.service.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tasteam.ai.analysis")
public class RestaurantReviewAnalysisPolicyProperties {

	private int summaryBatchSize = 10;
	private int comparisonMinReviews = 10;
	private int comparisonBatchSize = 10;
}
