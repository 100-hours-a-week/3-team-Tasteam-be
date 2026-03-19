package com.tasteam.domain.group.event;

import java.time.Instant;

public record GroupRequestSubmittedEvent(
	Long groupId,
	Long applicantMemberId,
	Long ownerId,
	String groupName,
	Instant submittedAt) {
}
