package com.tasteam.domain.promotion.entity;

import java.time.Instant;

public enum DisplayStatus {
	HIDDEN,
	SCHEDULED,
	DISPLAYING,
	DISPLAY_ENDED;

	public static DisplayStatus calculate(
		boolean displayEnabled,
		PublishStatus publishStatus,
		Instant displayStartAt,
		Instant displayEndAt,
		Instant now) {
		if (!displayEnabled || publishStatus != PublishStatus.PUBLISHED) {
			return HIDDEN;
		}
		if (now.isBefore(displayStartAt)) {
			return SCHEDULED;
		}
		if (now.isAfter(displayEndAt)) {
			return DISPLAY_ENDED;
		}
		return DISPLAYING;
	}
}
