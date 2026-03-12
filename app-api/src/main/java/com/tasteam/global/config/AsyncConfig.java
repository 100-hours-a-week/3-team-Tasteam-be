package com.tasteam.global.config;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.tasteam.global.aop.ObservedExecutor;

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

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, params) -> log.error("비동기 메서드 {}에서 예외 발생: {}", method.getName(),
			ex.getMessage(), ex);
	}
}
