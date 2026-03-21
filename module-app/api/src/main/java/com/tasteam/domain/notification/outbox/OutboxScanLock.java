package com.tasteam.domain.notification.outbox;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxScanLock {

	private static final String LOCK_KEY = "lock:notification:outbox-scan";
	private static final Duration LOCK_TTL = Duration.ofSeconds(25);

	private final StringRedisTemplate redisTemplate;

	public boolean tryLock() {
		Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", LOCK_TTL);
		return Boolean.TRUE.equals(acquired);
	}
}
