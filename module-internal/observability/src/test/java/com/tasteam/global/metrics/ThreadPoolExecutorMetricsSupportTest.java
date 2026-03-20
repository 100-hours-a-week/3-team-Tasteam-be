package com.tasteam.global.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@org.junit.jupiter.api.Tag("unit")
@DisplayName("[유닛](Metrics) ThreadPoolExecutorMetricsSupport 단위 테스트")
class ThreadPoolExecutorMetricsSupportTest {

	@Test
	@DisplayName("executor가 포화되면 queue utilization과 rejected counter를 기록한다")
	void bind_recordsQueueUtilizationAndRejectedTasks() throws InterruptedException {
		// Given
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		ThreadPoolExecutorMetricsSupport support = new ThreadPoolExecutorMetricsSupport();
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(1);
		executor.setThreadNamePrefix("metrics-test-");
		executor.setRejectedExecutionHandler(support.rejectedExecutionHandler(registry, "test_executor"));
		executor.initialize();
		support.bind(registry, executor, "test_executor");

		CountDownLatch workerStarted = new CountDownLatch(1);
		CountDownLatch releaseWorker = new CountDownLatch(1);

		// When
		executor.execute(() -> await(workerStarted, releaseWorker));
		assertThat(workerStarted.await(2, TimeUnit.SECONDS)).isTrue();
		executor.execute(() -> await(new CountDownLatch(0), releaseWorker));

		Gauge queueGauge = registry.find("executor.queue.utilization")
			.tag("executor", "test_executor")
			.gauge();

		// Then
		assertThat(queueGauge).isNotNull();
		assertThat(queueGauge.value()).isEqualTo(1.0);
		assertThatThrownBy(() -> executor.execute(() -> {}))
			.isInstanceOf(TaskRejectedException.class);

		Counter rejectedCounter = registry.find("executor.rejected.tasks")
			.tag("executor", "test_executor")
			.counter();
		assertThat(rejectedCounter).isNotNull();
		assertThat(rejectedCounter.count()).isEqualTo(1.0);

		releaseWorker.countDown();
		executor.shutdown();
		assertThat(executor.getThreadPoolExecutor().awaitTermination(Duration.ofSeconds(2).toMillis(),
			TimeUnit.MILLISECONDS)).isTrue();
	}

	private void await(CountDownLatch workerStarted, CountDownLatch releaseWorker) {
		try {
			workerStarted.countDown();
			releaseWorker.await(2, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("executor 테스트 작업이 인터럽트되었습니다.", ex);
		}
	}
}
