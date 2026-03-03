package com.tasteam.infra.messagequeue;

public record GroupMemberJoinedMessagePayload(
	Long groupId,
	Long memberId,
	String groupName,
	long joinedAtEpochMillis) {
}
