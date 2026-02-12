package com.tasteam.infra.redis;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
	public boolean setIfAbsent(String key, Object value) {
		Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value);
		return result != null && result;
	}

	@Override
	public boolean setIfAbsent(String key, Object value, Duration ttl) {
		Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
		return result != null && result;
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
	public void deleteAll(Collection<String> keys) {
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
	public Long getExpire(String key) {
		return redisTemplate.getExpire(key, TimeUnit.SECONDS);
	}

	@Override
	public Set<String> keys(String pattern) {
		return redisTemplate.keys(pattern);
	}

	@Override
	public void hSet(String key, String field, Object value) {
		redisTemplate.opsForHash().put(key, field, value);
	}

	@Override
	public void hSetAll(String key, Map<String, Object> map) {
		redisTemplate.opsForHash().putAll(key, map);
	}

	@Override
	public <T> Optional<T> hGet(String key, String field, Class<T> type) {
		Object value = redisTemplate.opsForHash().get(key, field);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(type.cast(value));
	}

	@Override
	public Map<Object, Object> hGetAll(String key) {
		return redisTemplate.opsForHash().entries(key);
	}

	@Override
	public void hDelete(String key, String... fields) {
		redisTemplate.opsForHash().delete(key, (Object[])fields);
	}

	@Override
	public boolean hExists(String key, String field) {
		return redisTemplate.opsForHash().hasKey(key, field);
	}

	@Override
	public void lPush(String key, Object value) {
		redisTemplate.opsForList().leftPush(key, value);
	}

	@Override
	public void rPush(String key, Object value) {
		redisTemplate.opsForList().rightPush(key, value);
	}

	@Override
	public <T> Optional<T> lPop(String key, Class<T> type) {
		Object value = redisTemplate.opsForList().leftPop(key);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(type.cast(value));
	}

	@Override
	public <T> Optional<T> rPop(String key, Class<T> type) {
		Object value = redisTemplate.opsForList().rightPop(key);
		if (value == null) {
			return Optional.empty();
		}
		return Optional.of(type.cast(value));
	}

	@Override
	public <T> List<T> lRange(String key, long start, long end, Class<T> type) {
		List<Object> values = redisTemplate.opsForList().range(key, start, end);
		if (values == null) {
			return List.of();
		}
		return values.stream()
			.map(type::cast)
			.collect(Collectors.toList());
	}

	@Override
	public Long lSize(String key) {
		return redisTemplate.opsForList().size(key);
	}

	@Override
	public void sAdd(String key, Object... values) {
		redisTemplate.opsForSet().add(key, values);
	}

	@Override
	public Set<Object> sMembers(String key) {
		return redisTemplate.opsForSet().members(key);
	}

	@Override
	public boolean sIsMember(String key, Object value) {
		Boolean result = redisTemplate.opsForSet().isMember(key, value);
		return result != null && result;
	}

	@Override
	public void sRemove(String key, Object... values) {
		redisTemplate.opsForSet().remove(key, values);
	}

	@Override
	public Long sSize(String key) {
		return redisTemplate.opsForSet().size(key);
	}

	@Override
	public Long increment(String key) {
		return redisTemplate.opsForValue().increment(key);
	}

	@Override
	public Long increment(String key, long delta) {
		return redisTemplate.opsForValue().increment(key, delta);
	}

	@Override
	public Long decrement(String key) {
		return redisTemplate.opsForValue().decrement(key);
	}

	@Override
	public Long decrement(String key, long delta) {
		return redisTemplate.opsForValue().decrement(key, delta);
	}
}
