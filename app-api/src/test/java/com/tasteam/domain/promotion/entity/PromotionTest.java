package com.tasteam.domain.promotion.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("프로모션 엔티티")
class PromotionTest {

	private static final String DEFAULT_TITLE = "신년 특가 프로모션";
	private static final String DEFAULT_CONTENT = "새해 맞이 특별 할인 행사";
	private static final String DEFAULT_LANDING_URL = "https://example.com/promotion";
	private static final Instant DEFAULT_START = Instant.parse("2026-01-01T00:00:00Z");
	private static final Instant DEFAULT_END = Instant.parse("2026-01-31T23:59:59Z");
	private static final PublishStatus DEFAULT_PUBLISH_STATUS = PublishStatus.PUBLISHED;

	private Promotion createPromotion() {
		return Promotion.create(
			DEFAULT_TITLE,
			DEFAULT_CONTENT,
			DEFAULT_LANDING_URL,
			DEFAULT_START,
			DEFAULT_END,
			DEFAULT_PUBLISH_STATUS);
	}

	@Nested
	@DisplayName("프로모션 생성")
	class CreatePromotion {

		@Test
		@DisplayName("프로모션을 생성하면 모든 필드가 정상적으로 설정된다")
		void create_validParams_createsPromotionWithAllFields() {
			Promotion promotion = createPromotion();

			assertThat(promotion.getTitle()).isEqualTo(DEFAULT_TITLE);
			assertThat(promotion.getContent()).isEqualTo(DEFAULT_CONTENT);
			assertThat(promotion.getLandingUrl()).isEqualTo(DEFAULT_LANDING_URL);
			assertThat(promotion.getPromotionStartAt()).isEqualTo(DEFAULT_START);
			assertThat(promotion.getPromotionEndAt()).isEqualTo(DEFAULT_END);
			assertThat(promotion.getPublishStatus()).isEqualTo(DEFAULT_PUBLISH_STATUS);
			assertThat(promotion.getDeletedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("프로모션 상태 조회")
	class GetPromotionStatus {

		@Test
		@DisplayName("현재 시간이 시작 전이면 UPCOMING을 반환한다")
		void getPromotionStatus_returnsUpcoming_whenBeforeStart() {
			Promotion promotion = Promotion.create(
				DEFAULT_TITLE,
				DEFAULT_CONTENT,
				DEFAULT_LANDING_URL,
				Instant.parse("2026-03-01T00:00:00Z"),
				Instant.parse("2026-03-31T23:59:59Z"),
				DEFAULT_PUBLISH_STATUS);

			assertThat(promotion.getPromotionStatus()).isEqualTo(PromotionStatus.UPCOMING);
		}

		@Test
		@DisplayName("현재 시간이 진행 중이면 ONGOING을 반환한다")
		void getPromotionStatus_returnsOngoing_whenInProgress() {
			Promotion promotion = Promotion.create(
				DEFAULT_TITLE,
				DEFAULT_CONTENT,
				DEFAULT_LANDING_URL,
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-12-31T23:59:59Z"),
				DEFAULT_PUBLISH_STATUS);

			assertThat(promotion.getPromotionStatus()).isEqualTo(PromotionStatus.ONGOING);
		}

		@Test
		@DisplayName("현재 시간이 종료 후면 ENDED를 반환한다")
		void getPromotionStatus_returnsEnded_whenAfterEnd() {
			Promotion promotion = Promotion.create(
				DEFAULT_TITLE,
				DEFAULT_CONTENT,
				DEFAULT_LANDING_URL,
				Instant.parse("2025-01-01T00:00:00Z"),
				Instant.parse("2025-01-31T23:59:59Z"),
				DEFAULT_PUBLISH_STATUS);

			assertThat(promotion.getPromotionStatus()).isEqualTo(PromotionStatus.ENDED);
		}
	}

	@Nested
	@DisplayName("프로모션 삭제")
	class DeletePromotion {

		@Test
		@DisplayName("프로모션을 삭제하면 deletedAt이 설정된다")
		void delete_setsDeletedAt() {
			Promotion promotion = createPromotion();
			Instant deletedAt = Instant.parse("2026-02-11T10:00:00Z");

			promotion.delete(deletedAt);

			assertThat(promotion.getDeletedAt()).isEqualTo(deletedAt);
		}
	}
}
