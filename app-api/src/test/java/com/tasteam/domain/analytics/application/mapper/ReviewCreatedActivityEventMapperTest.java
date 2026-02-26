package com.tasteam.domain.analytics.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.review.event.ReviewCreatedEvent;

@UnitTest
@DisplayName("리뷰 생성 사용자 이벤트 매퍼")
class ReviewCreatedActivityEventMapperTest {

	private final ReviewCreatedActivityEventMapper mapper = new ReviewCreatedActivityEventMapper();

	@Test
	@DisplayName("리뷰 생성 이벤트를 정규화 이벤트로 변환한다")
	void map_convertsReviewCreatedEventToActivityEvent() {
		// given
		ReviewCreatedEvent event = new ReviewCreatedEvent(33L);

		// when
		ActivityEvent mapped = mapper.map(event);

		// then
		assertThat(mapped.eventId()).isNotBlank();
		assertThat(mapped.eventName()).isEqualTo("review.created");
		assertThat(mapped.eventVersion()).isEqualTo("v1");
		assertThat(mapped.memberId()).isNull();
		assertThat(mapped.occurredAt()).isNotNull();
		assertThat(mapped.properties())
			.containsEntry("restaurantId", 33L);
	}
}
