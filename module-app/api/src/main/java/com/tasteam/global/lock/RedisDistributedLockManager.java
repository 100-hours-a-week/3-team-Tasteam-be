package com.tasteam.global.lock;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisDistributedLockManager {

	private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
		"""
			if redis.call('get', KEYS[1]) == ARGV[1] then
			    return redis.call('del', KEYS[1])
			end
			return 0
			""",
		Long.class);

	private final StringRedisTemplate redisTemplate;

	public Optional<LockHandle> tryLock(String key, Duration ttl) {
		String token = UUID.randomUUID().toString();
		Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, ttl);
		if (!Boolean.TRUE.equals(acquired)) {
			return Optional.empty();
		}
		return Optional.of(new LockHandle(redisTemplate, key, token));
	}

	public static final class LockHandle implements AutoCloseable {

		private final StringRedisTemplate redisTemplate;
		private final String key;
		private final String token;
		private final AtomicBoolean released = new AtomicBoolean(false);

		private LockHandle(StringRedisTemplate redisTemplate, String key, String token) {
			this.redisTemplate = redisTemplate;
			this.key = key;
			this.token = token;
		}

		@Override
		public void close() {
			if (!released.compareAndSet(false, true)) {
				return;
			}
			redisTemplate.execute(RELEASE_SCRIPT, List.of(key), token);
		}
	}
}
