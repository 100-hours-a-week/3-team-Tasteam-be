package com.tasteam.domain.subgroup.event;

import java.time.Instant;

public record SubgroupMemberJoinedEvent(
	Long groupId,
	Long subgroupId,
	Long memberId,
	String subgroupName,
	Instant joinedAt) {
}
