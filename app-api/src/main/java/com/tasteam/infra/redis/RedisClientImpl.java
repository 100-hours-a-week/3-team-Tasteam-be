package com.tasteam.infra.redis;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisClientImpl implements RedisClient {

	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	public void set(String key, Object value) {
		redisTemplate.opsForValue().set(key, value);
	}

	@Override
	public void set(String key, Object value, Duration ttl) {
		redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public <T> Optional<T> get(String key, Class<T> type) {
		Object value = redisTemplate.opsForValue().get(key);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(type.cast(value));
	}

	@Override
	public void delete(String key) {
		redisTemplate.delete(key);
	}

	@Override
	public void deleteAll(Set<String> keys) {
		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}
	}

	@Override
	public boolean exists(String key) {
		Boolean result = redisTemplate.hasKey(key);
		return result != null && result;
	}

	@Override
	public void expire(String key, Duration ttl) {
		redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public Set<String> keys(String pattern) {
		return redisTemplate.keys(pattern);
	}
}
