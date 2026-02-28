package com.tasteam.global.ratelimit;

public record RateLimitRequest(
	String email,
	String ip,
	Long userId) {
}
