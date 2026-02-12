package com.tasteam.infra.redis;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public interface RedisClient {

	void set(String key, Object value);

	void set(String key, Object value, Duration ttl);

	<T> Optional<T> get(String key, Class<T> type);

	void delete(String key);

	void deleteAll(Set<String> keys);

	boolean exists(String key);

	void expire(String key, Duration ttl);

	Set<String> keys(String pattern);
}
