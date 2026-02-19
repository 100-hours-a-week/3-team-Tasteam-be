package com.tasteam.domain.analytics.ingest;

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
@DisplayName("클라이언트 이벤트 수집 요청 레이트리미터")
class ClientActivityIngestRateLimiterTest {

	@Test
	@DisplayName("같은 윈도우에서 허용 횟수를 초과하면 요청을 차단한다")
	void tryAcquire_blocksWhenRequestCountExceededWithinWindow() {
		// given
		MutableClock clock = new MutableClock(Instant.parse("2026-02-19T00:00:00Z"));
		ClientActivityIngestRateLimiter limiter = new ClientActivityIngestRateLimiter(2, Duration.ofMinutes(1), clock);

		// when
		boolean first = limiter.tryAcquire("member:1");
		boolean second = limiter.tryAcquire("member:1");
		boolean third = limiter.tryAcquire("member:1");

		// then
		assertThat(first).isTrue();
		assertThat(second).isTrue();
		assertThat(third).isFalse();
	}

	@Test
	@DisplayName("윈도우가 지나면 카운터가 초기화되어 다시 요청을 허용한다")
	void tryAcquire_allowsAgainWhenWindowExpired() {
		// given
		MutableClock clock = new MutableClock(Instant.parse("2026-02-19T00:00:00Z"));
		ClientActivityIngestRateLimiter limiter = new ClientActivityIngestRateLimiter(1, Duration.ofSeconds(30), clock);
		assertThat(limiter.tryAcquire("anonymous:a-1")).isTrue();
		assertThat(limiter.tryAcquire("anonymous:a-1")).isFalse();

		// when
		clock.advance(Duration.ofSeconds(31));
		boolean afterWindow = limiter.tryAcquire("anonymous:a-1");

		// then
		assertThat(afterWindow).isTrue();
	}

	private static final class MutableClock extends Clock {

		private Instant current;

		private MutableClock(Instant current) {
			this.current = current;
		}

		private void advance(Duration duration) {
			this.current = this.current.plus(duration);
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
