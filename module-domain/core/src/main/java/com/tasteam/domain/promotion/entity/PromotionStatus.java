package com.tasteam.domain.promotion.entity;

import java.time.Instant;

public enum PromotionStatus {
	UPCOMING,
	ONGOING,
	ENDED;

	public static PromotionStatus calculate(Instant promotionStartAt, Instant promotionEndAt, Instant now) {
		if (now.isBefore(promotionStartAt)) {
			return UPCOMING;
		}
		if (now.isAfter(promotionEndAt)) {
			return ENDED;
		}
		return ONGOING;
	}
}
