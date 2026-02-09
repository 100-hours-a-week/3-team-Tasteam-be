package com.tasteam.global.config;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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

	@Bean(name = "searchHistoryExecutor")
	public Executor searchHistoryExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("search-history-");
		executor.initialize();
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, params) -> log.error("비동기 메서드 {}에서 예외 발생: {}", method.getName(),
			ex.getMessage(), ex);
	}
}
