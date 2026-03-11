package com.tasteam.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
	public Executor notificationExecutor() {
		return new TaskExecutorAdapter(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
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
	public Executor searchHistoryExecutor(MeterRegistry registry) {
		String executorName = "search_history";
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("search-history-");
		executor.setRejectedExecutionHandler(buildRejectedExecutionHandler(registry, executorName));
		executor.initialize();
		ExecutorServiceMetrics.monitor(registry, executor.getThreadPoolExecutor(), executorName);
		registerQueueUtilizationGauge(registry, executor, executorName);
		return executor;
	}

	@Bean(name = "aiAnalysisExecutor")
	public Executor aiAnalysisExecutor(MeterRegistry registry) {
		String executorName = "ai_analysis";
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(1);
		executor.setMaxPoolSize(1);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("ai-analysis-");
		executor.setRejectedExecutionHandler(buildRejectedExecutionHandler(registry, executorName));
		executor.initialize();
		ExecutorServiceMetrics.monitor(registry, executor.getThreadPoolExecutor(), executorName);
		registerQueueUtilizationGauge(registry, executor, executorName);
		return executor;
	}

	@Bean("dummySeedExecutor")
	public Executor dummySeedExecutor(MeterRegistry registry) {
		String executorName = "dummy_seed";
		ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
		exec.setCorePoolSize(1);
		exec.setMaxPoolSize(1);
		exec.setQueueCapacity(0);
		exec.setRejectedExecutionHandler(buildRejectedExecutionHandler(registry, executorName));
		exec.setThreadNamePrefix("dummy-seed-");
		exec.initialize();
		registerQueueUtilizationGauge(registry, exec, executorName);
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
