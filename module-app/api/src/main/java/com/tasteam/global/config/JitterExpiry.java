package com.tasteam.global.config;

import java.util.concurrent.ThreadLocalRandom;

import com.github.benmanes.caffeine.cache.Expiry;

class JitterExpiry implements Expiry<Object, Object> {

	private final long baseNanos;
	private final long jitterNanos;

	JitterExpiry(long baseNanos, long jitterNanos) {
		this.baseNanos = baseNanos;
		this.jitterNanos = jitterNanos;
	}

	@Override
	public long expireAfterCreate(Object key, Object value, long currentTime) {
		if (jitterNanos <= 0) {
			return baseNanos;
		}
		return baseNanos + ThreadLocalRandom.current().nextLong(jitterNanos + 1);
	}

	@Override
	public long expireAfterUpdate(Object key, Object value, long currentTime, long currentDuration) {
		return expireAfterCreate(key, value, currentTime);
	}

	@Override
	public long expireAfterRead(Object key, Object value, long currentTime, long currentDuration) {
		return currentDuration;
	}
}
