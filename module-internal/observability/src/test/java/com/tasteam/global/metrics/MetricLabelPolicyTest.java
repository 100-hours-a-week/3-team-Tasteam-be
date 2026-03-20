package com.tasteam.global.metrics;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("[유닛](Metrics) MetricLabelPolicy 단위 테스트")
class MetricLabelPolicyTest {

	@Test
	@DisplayName("허용된 라벨은 검증을 통과한다")
	void validate_passesWhitelistedLabels() {
		assertThatCode(() -> MetricLabelPolicy.validate("metric.sample",
			"result", "success",
			"state", "open",
			"target", "posthog",
			"executor", "search_history")).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("금지된 라벨은 예외가 발생한다")
	void validate_failsWhenForbiddenLabelUsed() {
		assertThatThrownBy(() -> MetricLabelPolicy.validate("metric.sample", "eventId", "evt-1"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("금지된 메트릭 라벨");
	}

	@Test
	@DisplayName("화이트리스트에 없는 라벨은 예외가 발생한다")
	void validate_failsWhenUnknownLabelUsed() {
		assertThatThrownBy(() -> MetricLabelPolicy.validate("metric.sample", "user_agent", "Mozilla"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("허용되지 않은 메트릭 라벨");
	}

	@Test
	@DisplayName("라벨 key/value 쌍이 맞지 않으면 예외가 발생한다")
	void validate_failsWhenTagPairIsBroken() {
		assertThatThrownBy(() -> MetricLabelPolicy.validate("metric.sample", "result"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("key/value");
	}
}
