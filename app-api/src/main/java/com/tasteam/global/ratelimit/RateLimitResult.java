package com.tasteam.global.ratelimit;

public record RateLimitResult(
	boolean allowed,
	RateLimitReason reason,
	long retryAfterSeconds) {

	public static RateLimitResult allow() {
		return new RateLimitResult(true, RateLimitReason.ALLOWED, 0L);
	}
}
