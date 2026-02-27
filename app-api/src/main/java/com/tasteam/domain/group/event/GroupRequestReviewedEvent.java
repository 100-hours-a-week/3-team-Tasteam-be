package com.tasteam.domain.group.event;

import java.time.Instant;

public record GroupRequestReviewedEvent(
	Long groupId,
	Long applicantMemberId,
	String groupName,
	ReviewResult result,
	String reason,
	Instant reviewedAt) {

	public enum ReviewResult {
		APPROVED, REJECTED
	}
}
