package com.tasteam.batch.ai.review.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(ReviewAnalysisBatchProperties.class)
public class ReviewAnalysisBatchConfig {

	@Bean(name = "reviewAnalysisExecutor")
	public Executor reviewAnalysisExecutor(ReviewAnalysisBatchProperties properties) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(properties.getWorkerPoolSize());
		executor.setMaxPoolSize(properties.getWorkerPoolSize());
		executor.setQueueCapacity(1000); // 0이면 워커 수 초과 시 RejectedExecutionException → 나머지 PENDING 유지
		executor.setThreadNamePrefix("review-analysis-");
		executor.initialize();
		return executor;
	}
}
