package com.tasteam.domain.analytics.application.mapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.api.ActivityEventMapper;
import com.tasteam.domain.group.event.GroupMemberJoinedEvent;

/**
 * 그룹 가입 도메인 이벤트를 정규화 사용자 활동 이벤트로 변환합니다.
 */
@Component
public class GroupMemberJoinedActivityEventMapper implements ActivityEventMapper<GroupMemberJoinedEvent> {

	@Override
	public Class<GroupMemberJoinedEvent> sourceType() {
		return GroupMemberJoinedEvent.class;
	}

	@Override
	public ActivityEvent map(GroupMemberJoinedEvent event) {
		Objects.requireNonNull(event, "event는 null일 수 없습니다.");

		Map<String, Object> properties = new LinkedHashMap<>();
		if (event.groupId() != null) {
			properties.put("groupId", event.groupId());
		}
		if (event.groupName() != null && !event.groupName().isBlank()) {
			properties.put("groupName", event.groupName());
		}

		return new ActivityEvent(
			UUID.randomUUID().toString(),
			"group.joined",
			"v1",
			resolveOccurredAt(event.joinedAt()),
			event.memberId(),
			null,
			properties);
	}

	private Instant resolveOccurredAt(Instant joinedAt) {
		return joinedAt == null ? Instant.now() : joinedAt;
	}
}
