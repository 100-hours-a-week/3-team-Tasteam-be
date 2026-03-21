package com.tasteam.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.tasteam.global.aop.ObservedExecutor;
import com.tasteam.global.metrics.MetricLabelPolicy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	@Bean(name = "webhookExecutor")
	public Executor webhookExecutor() {
		return new TaskExecutorAdapter(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
	}

	@Bean(name = "notificationExecutor")
	@ObservedExecutor(name = "notification")
	public Executor notificationExecutor(
		@Value("${tasteam.notification.executor.core-pool-size:8}")
		int corePoolSize,
		@Value("${tasteam.notification.executor.max-pool-size:32}")
		int maxPoolSize,
		@Value("${tasteam.notification.executor.queue-capacity:1000}")
		int queueCapacity,
		@Nullable
		MeterRegistry meterRegistry) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(Math.max(1, corePoolSize));
		executor.setMaxPoolSize(Math.max(Math.max(1, corePoolSize), maxPoolSize));
		executor.setQueueCapacity(Math.max(0, queueCapacity));
		executor.setThreadNamePrefix("notification-");
		if (meterRegistry != null) {
			meterRegistry.gauge(
				"notification.executor.queue.size",
				executor,
				this::resolveQueueSize);
		}
		return executor;
	}

	@Bean(name = "searchQueryExecutor")
	@ConditionalOnMissingBean(name = "searchQueryExecutor")
	public Executor searchQueryExecutor() {
		return new TaskExecutorAdapter(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
	}

	@Bean(name = "mainQueryExecutor")
	@ConditionalOnMissingBean(name = "mainQueryExecutor")
	public Executor mainQueryExecutor() {
		return new TaskExecutorAdapter(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
	}

	@Bean(name = "searchHistoryExecutor")
	@ObservedExecutor(name = "search_history")
	public Executor searchHistoryExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("search-history-");
		return executor;
	}

	@Bean(name = "aiAnalysisExecutor")
	@ObservedExecutor(name = "ai_analysis")
	public Executor aiAnalysisExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("ai-analysis-");
		return executor;
	}

	@Bean("dummySeedExecutor")
	@ObservedExecutor(name = "dummy_seed")
	public Executor dummySeedExecutor() {
		ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
		exec.setCorePoolSize(1);
		exec.setMaxPoolSize(1);
		exec.setQueueCapacity(0);
		exec.setThreadNamePrefix("dummy-seed-");
		return exec;
	}

	@Bean(name = "rawDataExportExecutor")
	public Executor rawDataExportExecutor(MeterRegistry registry) {
		String executorName = "raw_data_export";
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(10);
		executor.setThreadNamePrefix("raw-data-export-");
		executor.setRejectedExecutionHandler(buildRejectedExecutionHandler(registry, executorName));
		executor.initialize();
		ExecutorServiceMetrics.monitor(registry, executor.getThreadPoolExecutor(), executorName);
		registerQueueUtilizationGauge(registry, executor, executorName);
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, params) -> log.error("비동기 메서드 {}에서 예외 발생: {}", method.getName(),
			ex.getMessage(), ex);
	}

	private RejectedExecutionHandler buildRejectedExecutionHandler(MeterRegistry registry, String executorName) {
		RejectedExecutionHandler fallback = new ThreadPoolExecutor.AbortPolicy();
		return (task, threadPoolExecutor) -> {
			MetricLabelPolicy.validate("executor.rejected.tasks", "executor", executorName);
			registry.counter("executor.rejected.tasks", "executor", executorName).increment();
			fallback.rejectedExecution(task, threadPoolExecutor);
		};
	}

	private void bindExecutorMetrics(MeterRegistry registry, ThreadPoolTaskExecutor executor, String executorName) {
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

	private double resolveQueueSize(ThreadPoolTaskExecutor executor) {
		if (executor == null) {
			return 0.0;
		}
		try {
			ThreadPoolExecutor delegate = executor.getThreadPoolExecutor();
			if (delegate == null) {
				return 0.0;
			}
			return delegate.getQueue().size();
		} catch (IllegalStateException ignored) {
			return 0.0;
		}
	}
}
