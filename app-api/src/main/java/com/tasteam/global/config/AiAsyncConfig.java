package com.tasteam.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.tasteam.global.aop.ObservedExecutor;

@Configuration
@EnableAsync
public class AiAsyncConfig {

	@Bean(name = "aiExecutor")
	@ObservedExecutor(name = "ai_executor")
	public Executor aiExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("ai-exec-");
		return executor;
	}
}
