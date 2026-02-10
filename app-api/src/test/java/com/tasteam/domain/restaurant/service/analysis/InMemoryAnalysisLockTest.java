package com.tasteam.domain.restaurant.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
class InMemoryAnalysisLockTest {

	private final InMemoryAnalysisLock lock = new InMemoryAnalysisLock();

	@Test
	@DisplayName("같은 restaurantId에 대해 중복 획득을 막는다")
	void tryLock_blocksDuplicate() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicBoolean secondAcquired = new AtomicBoolean(true);

		assertThat(lock.tryLock(100L)).isTrue();
		Thread second = new Thread(() -> {
			secondAcquired.set(lock.tryLock(100L));
			latch.countDown();
		});
		second.start();
		assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
		assertThat(secondAcquired.get()).isFalse();

		lock.unlock(100L);
		assertThat(lock.tryLock(100L)).isTrue();
		lock.unlock(100L);
	}
}
