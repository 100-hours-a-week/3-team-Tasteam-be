package com.tasteam.domain.admin.dto.request;

import java.time.Instant;

public record AdminUserActivityEventSearchCondition(
	String eventName,
	String source,
	Long memberId,
	String platform,
	Instant occurredAtFrom,
	Instant occurredAtTo) {
}
