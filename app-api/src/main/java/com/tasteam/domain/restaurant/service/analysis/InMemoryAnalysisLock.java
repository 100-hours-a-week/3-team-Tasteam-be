package com.tasteam.domain.restaurant.service.analysis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

@Component
public class InMemoryAnalysisLock implements AnalysisLock {

	private final ConcurrentMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

	@Override
	public boolean tryLock(long restaurantId) {
		ReentrantLock lock = locks.computeIfAbsent(restaurantId, ignored -> new ReentrantLock());
		boolean acquired = lock.tryLock();
		if (!acquired && !lock.isLocked() && !lock.hasQueuedThreads()) {
			locks.remove(restaurantId, lock);
		}
		return acquired;
	}

	@Override
	public void unlock(long restaurantId) {
		ReentrantLock lock = locks.get(restaurantId);
		if (lock == null) {
			return;
		}
		if (lock.isHeldByCurrentThread()) {
			lock.unlock();
		}
		if (!lock.isLocked() && !lock.hasQueuedThreads()) {
			locks.remove(restaurantId, lock);
		}
	}
}
