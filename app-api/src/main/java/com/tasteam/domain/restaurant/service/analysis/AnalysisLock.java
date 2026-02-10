package com.tasteam.domain.restaurant.service.analysis;

public interface AnalysisLock {
	boolean tryLock(long restaurantId);

	void unlock(long restaurantId);
}
