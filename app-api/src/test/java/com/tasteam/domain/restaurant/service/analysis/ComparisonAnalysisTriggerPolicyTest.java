package com.tasteam.domain.restaurant.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
class ComparisonAnalysisTriggerPolicyTest {

	private final RestaurantReviewAnalysisPolicyProperties properties = createProperties();
	private final ComparisonAnalysisTriggerPolicy policy = new ComparisonAnalysisTriggerPolicy(properties);

	private RestaurantReviewAnalysisPolicyProperties createProperties() {
		RestaurantReviewAnalysisPolicyProperties props = new RestaurantReviewAnalysisPolicyProperties();
		props.setComparisonMinReviews(10);
		props.setComparisonBatchSize(10);
		return props;
	}

	@Test
	@DisplayName("리뷰 수가 최소 조건 미만이면 실행하지 않는다")
	void shouldRun_belowMinimum_false() {
		assertThat(policy.shouldRun(9)).isFalse();
	}

	@Test
	@DisplayName("리뷰 수가 최소 조건 이상이면 10개 단위로만 실행한다")
	void shouldRun_aboveMinimum_everyTen() {
		assertThat(policy.shouldRun(10)).isTrue();
		assertThat(policy.shouldRun(11)).isFalse();
		assertThat(policy.shouldRun(20)).isTrue();
	}
}
