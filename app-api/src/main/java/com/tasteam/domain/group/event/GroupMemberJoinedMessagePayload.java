package com.tasteam.domain.group.event;

public record GroupMemberJoinedMessagePayload(
	Long groupId,
	Long memberId,
	String groupName,
	long joinedAtEpochMillis) {
}
