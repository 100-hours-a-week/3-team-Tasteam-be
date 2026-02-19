package com.tasteam.domain.analytics.application.mapper;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.api.ActivityEventMapper;
import com.tasteam.domain.review.event.ReviewCreatedEvent;

/**
 * 리뷰 생성 도메인 이벤트를 정규화 사용자 활동 이벤트로 변환합니다.
 */
@Component
public class ReviewCreatedActivityEventMapper implements ActivityEventMapper<ReviewCreatedEvent> {

	@Override
	public Class<ReviewCreatedEvent> sourceType() {
		return ReviewCreatedEvent.class;
	}

	@Override
	public ActivityEvent map(ReviewCreatedEvent event) {
		Objects.requireNonNull(event, "event는 null일 수 없습니다.");
		return new ActivityEvent(
			UUID.randomUUID().toString(),
			"review.created",
			"v1",
			Instant.now(),
			null,
			null,
			Map.of("restaurantId", event.restaurantId()));
	}
}
