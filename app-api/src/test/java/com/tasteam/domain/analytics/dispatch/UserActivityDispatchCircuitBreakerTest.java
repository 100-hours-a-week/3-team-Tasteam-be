package com.tasteam.domain.analytics.dispatch;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("사용자 이벤트 dispatch 서킷 브레이커")
class UserActivityDispatchCircuitBreakerTest {

	@Test
	@DisplayName("연속 실패 임계치에 도달하면 서킷이 열린다")
	void recordFailure_opensCircuitWhenThresholdReached() {
		// given
		MutableClock clock = new MutableClock(Instant.parse("2026-02-19T00:00:00Z"));
		UserActivityDispatchCircuitBreaker circuitBreaker = new UserActivityDispatchCircuitBreaker(
			2,
			Duration.ofSeconds(30),
			clock);

		// when
		circuitBreaker.recordFailure();
		boolean afterFirstFailure = circuitBreaker.isOpen();
		circuitBreaker.recordFailure();

		// then
		assertThat(afterFirstFailure).isFalse();
		assertThat(circuitBreaker.isOpen()).isTrue();
		assertThat(circuitBreaker.allowRequest()).isFalse();
	}

	@Test
	@DisplayName("서킷 오픈 시간이 지나면 자동으로 닫힌다")
	void allowRequest_closesCircuitAfterOpenDuration() {
		// given
		MutableClock clock = new MutableClock(Instant.parse("2026-02-19T00:00:00Z"));
		UserActivityDispatchCircuitBreaker circuitBreaker = new UserActivityDispatchCircuitBreaker(
			1,
			Duration.ofSeconds(20),
			clock);
		circuitBreaker.recordFailure();
		assertThat(circuitBreaker.isOpen()).isTrue();

		// when
		clock.advance(Duration.ofSeconds(21));

		// then
		assertThat(circuitBreaker.allowRequest()).isTrue();
		assertThat(circuitBreaker.isOpen()).isFalse();
	}

	private static final class MutableClock extends Clock {

		private Instant current;

		private MutableClock(Instant current) {
			this.current = current;
		}

		void advance(Duration duration) {
			current = current.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return current;
		}
	}
}
