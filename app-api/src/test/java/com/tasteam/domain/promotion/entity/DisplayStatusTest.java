package com.tasteam.domain.promotion.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("노출 상태")
class DisplayStatusTest {

	private static final Instant BASE_TIME = Instant.parse("2026-02-01T12:00:00Z");
	private static final Instant DISPLAY_START = Instant.parse("2026-02-01T00:00:00Z");
	private static final Instant DISPLAY_END = Instant.parse("2026-02-28T23:59:59Z");

	@Nested
	@DisplayName("상태 계산")
	class CalculateStatus {

		@Test
		@DisplayName("노출이 비활성화되면 HIDDEN을 반환한다")
		void calculate_returnsHidden_whenDisplayDisabled() {
			DisplayStatus status = DisplayStatus.calculate(
				false,
				PublishStatus.PUBLISHED,
				DISPLAY_START,
				DISPLAY_END,
				BASE_TIME);

			assertThat(status).isEqualTo(DisplayStatus.HIDDEN);
		}

		@Test
		@DisplayName("발행 상태가 DRAFT면 HIDDEN을 반환한다")
		void calculate_returnsHidden_whenDraft() {
			DisplayStatus status = DisplayStatus.calculate(
				true,
				PublishStatus.DRAFT,
				DISPLAY_START,
				DISPLAY_END,
				BASE_TIME);

			assertThat(status).isEqualTo(DisplayStatus.HIDDEN);
		}

		@Test
		@DisplayName("발행 상태가 ARCHIVED면 HIDDEN을 반환한다")
		void calculate_returnsHidden_whenArchived() {
			DisplayStatus status = DisplayStatus.calculate(
				true,
				PublishStatus.ARCHIVED,
				DISPLAY_START,
				DISPLAY_END,
				BASE_TIME);

			assertThat(status).isEqualTo(DisplayStatus.HIDDEN);
		}

		@Test
		@DisplayName("노출 시작 전이면 SCHEDULED를 반환한다")
		void calculate_returnsScheduled_whenBeforeDisplayStart() {
			Instant now = Instant.parse("2026-01-31T12:00:00Z");

			DisplayStatus status = DisplayStatus.calculate(
				true,
				PublishStatus.PUBLISHED,
				DISPLAY_START,
				DISPLAY_END,
				now);

			assertThat(status).isEqualTo(DisplayStatus.SCHEDULED);
		}

		@Test
		@DisplayName("노출 중이면 DISPLAYING을 반환한다")
		void calculate_returnsDisplaying_whenInDisplayPeriod() {
			Instant now = Instant.parse("2026-02-15T12:00:00Z");

			DisplayStatus status = DisplayStatus.calculate(
				true,
				PublishStatus.PUBLISHED,
				DISPLAY_START,
				DISPLAY_END,
				now);

			assertThat(status).isEqualTo(DisplayStatus.DISPLAYING);
		}

		@Test
		@DisplayName("노출 시작 시간과 동일하면 DISPLAYING을 반환한다")
		void calculate_returnsDisplaying_whenEqualsDisplayStart() {
			DisplayStatus status = DisplayStatus.calculate(
				true,
				PublishStatus.PUBLISHED,
				DISPLAY_START,
				DISPLAY_END,
				DISPLAY_START);

			assertThat(status).isEqualTo(DisplayStatus.DISPLAYING);
		}

		@Test
		@DisplayName("노출 종료 시간과 동일하면 DISPLAYING을 반환한다")
		void calculate_returnsDisplaying_whenEqualsDisplayEnd() {
			DisplayStatus status = DisplayStatus.calculate(
				true,
				PublishStatus.PUBLISHED,
				DISPLAY_START,
				DISPLAY_END,
				DISPLAY_END);

			assertThat(status).isEqualTo(DisplayStatus.DISPLAYING);
		}

		@Test
		@DisplayName("노출 종료 후면 DISPLAY_ENDED를 반환한다")
		void calculate_returnsDisplayEnded_whenAfterDisplayEnd() {
			Instant now = Instant.parse("2026-03-01T00:00:00Z");

			DisplayStatus status = DisplayStatus.calculate(
				true,
				PublishStatus.PUBLISHED,
				DISPLAY_START,
				DISPLAY_END,
				now);

			assertThat(status).isEqualTo(DisplayStatus.DISPLAY_ENDED);
		}
	}
}
