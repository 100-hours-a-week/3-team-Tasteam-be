package com.tasteam.domain.analytics.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.group.event.GroupMemberJoinedEvent;

@UnitTest
@DisplayName("그룹 가입 사용자 이벤트 매퍼")
class GroupMemberJoinedActivityEventMapperTest {

	private final GroupMemberJoinedActivityEventMapper mapper = new GroupMemberJoinedActivityEventMapper();

	@Test
	@DisplayName("그룹 가입 이벤트를 정규화 이벤트로 변환한다")
	void map_convertsGroupMemberJoinedEventToActivityEvent() {
		// given
		Instant joinedAt = Instant.parse("2026-02-18T07:00:00Z");
		GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(7L, 9L, "강남 점심팟", joinedAt);

		// when
		ActivityEvent mapped = mapper.map(event);

		// then
		assertThat(mapped.eventId()).isNotBlank();
		assertThat(mapped.eventName()).isEqualTo("group.joined");
		assertThat(mapped.eventVersion()).isEqualTo("v1");
		assertThat(mapped.memberId()).isEqualTo(9L);
		assertThat(mapped.occurredAt()).isEqualTo(joinedAt);
		assertThat(mapped.properties())
			.containsEntry("groupId", 7L)
			.containsEntry("groupName", "강남 점심팟");
	}

	@Test
	@DisplayName("joinedAt이 없으면 현재 시각 기준으로 occurredAt을 채운다")
	void map_setsOccurredAtNowWhenJoinedAtIsNull() {
		// given
		GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(7L, 9L, "강남 점심팟", null);
		Instant before = Instant.now();

		// when
		ActivityEvent mapped = mapper.map(event);
		Instant after = Instant.now();

		// then
		assertThat(mapped.occurredAt()).isBetween(before, after);
	}
}
