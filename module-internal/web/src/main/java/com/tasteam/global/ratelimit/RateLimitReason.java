package com.tasteam.global.ratelimit;

public enum RateLimitReason {
	ALLOWED,
	EMAIL_BLOCKED_24H,
	RATE_LIMIT_EMAIL_1M,
	RATE_LIMIT_IP_1M,
	RATE_LIMIT_USER_1M,
	RATE_LIMITER_UNAVAILABLE;

	public static RateLimitReason from(String value) {
		if (value == null) {
			return RATE_LIMITER_UNAVAILABLE;
		}
		try {
			return RateLimitReason.valueOf(value);
		} catch (IllegalArgumentException e) {
			return RATE_LIMITER_UNAVAILABLE;
		}
	}
}
