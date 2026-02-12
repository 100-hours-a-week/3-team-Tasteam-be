package com.tasteam.domain.promotion.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("프로모션 상태")
class PromotionStatusTest {

	private static final Instant BASE_TIME = Instant.parse("2026-02-01T12:00:00Z");
	private static final Instant START_TIME = Instant.parse("2026-02-01T00:00:00Z");
	private static final Instant END_TIME = Instant.parse("2026-02-28T23:59:59Z");

	@Nested
	@DisplayName("상태 계산")
	class CalculateStatus {

		@Test
		@DisplayName("시작 전이면 UPCOMING을 반환한다")
		void calculate_returnsUpcoming_whenBeforeStart() {
			Instant now = Instant.parse("2026-01-31T12:00:00Z");

			PromotionStatus status = PromotionStatus.calculate(START_TIME, END_TIME, now);

			assertThat(status).isEqualTo(PromotionStatus.UPCOMING);
		}

		@Test
		@DisplayName("진행 중이면 ONGOING을 반환한다")
		void calculate_returnsOngoing_whenInProgress() {
			Instant now = Instant.parse("2026-02-15T12:00:00Z");

			PromotionStatus status = PromotionStatus.calculate(START_TIME, END_TIME, now);

			assertThat(status).isEqualTo(PromotionStatus.ONGOING);
		}

		@Test
		@DisplayName("시작 시간과 동일하면 ONGOING을 반환한다")
		void calculate_returnsOngoing_whenEqualsStart() {
			PromotionStatus status = PromotionStatus.calculate(START_TIME, END_TIME, START_TIME);

			assertThat(status).isEqualTo(PromotionStatus.ONGOING);
		}

		@Test
		@DisplayName("종료 시간과 동일하면 ONGOING을 반환한다")
		void calculate_returnsOngoing_whenEqualsEnd() {
			PromotionStatus status = PromotionStatus.calculate(START_TIME, END_TIME, END_TIME);

			assertThat(status).isEqualTo(PromotionStatus.ONGOING);
		}

		@Test
		@DisplayName("종료 후면 ENDED를 반환한다")
		void calculate_returnsEnded_whenAfterEnd() {
			Instant now = Instant.parse("2026-03-01T00:00:00Z");

			PromotionStatus status = PromotionStatus.calculate(START_TIME, END_TIME, now);

			assertThat(status).isEqualTo(PromotionStatus.ENDED);
		}
	}
}
