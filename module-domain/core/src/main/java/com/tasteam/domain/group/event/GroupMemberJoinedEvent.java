package com.tasteam.domain.group.event;

import java.time.Instant;

public record GroupMemberJoinedEvent(
	Long groupId,
	Long memberId,
	String groupName,
	Instant joinedAt) {
}
