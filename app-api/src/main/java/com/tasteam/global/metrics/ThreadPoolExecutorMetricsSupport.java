package com.tasteam.global.metrics;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

@Component
public class ThreadPoolExecutorMetricsSupport {

	public RejectedExecutionHandler rejectedExecutionHandler(MeterRegistry registry, String executorName) {
		RejectedExecutionHandler fallback = new ThreadPoolExecutor.AbortPolicy();
		return (task, threadPoolExecutor) -> {
			MetricLabelPolicy.validate("executor.rejected.tasks", "executor", executorName);
			registry.counter("executor.rejected.tasks", "executor", executorName).increment();
			fallback.rejectedExecution(task, threadPoolExecutor);
		};
	}

	public void bind(MeterRegistry registry, ThreadPoolTaskExecutor executor, String executorName) {
		ExecutorServiceMetrics.monitor(registry, executor.getThreadPoolExecutor(), executorName);
		registerQueueUtilizationGauge(registry, executor, executorName);
	}

	private void registerQueueUtilizationGauge(
		MeterRegistry registry,
		ThreadPoolTaskExecutor executor,
		String executorName) {
		MetricLabelPolicy.validate("executor.queue.utilization", "executor", executorName);
		registry.gauge("executor.queue.utilization",
			Tags.of("executor", executorName),
			executor,
			threadPoolTaskExecutor -> {
				ThreadPoolExecutor delegate = threadPoolTaskExecutor.getThreadPoolExecutor();
				if (delegate == null) {
					return 0.0;
				}
				int queueSize = delegate.getQueue().size();
				int queueCapacity = queueSize + delegate.getQueue().remainingCapacity();
				if (queueCapacity <= 0) {
					return 0.0;
				}
				return (double)queueSize / queueCapacity;
			});
	}
}
