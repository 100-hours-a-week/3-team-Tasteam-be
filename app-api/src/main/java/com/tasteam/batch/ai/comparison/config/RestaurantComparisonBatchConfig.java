package com.tasteam.batch.ai.comparison.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(RestaurantComparisonBatchProperties.class)
public class RestaurantComparisonBatchConfig {

	@Bean(name = "restaurantComparisonExecutor")
	public Executor restaurantComparisonExecutor(RestaurantComparisonBatchProperties properties) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(properties.getWorkerPoolSize());
		executor.setMaxPoolSize(properties.getWorkerPoolSize());
		executor.setQueueCapacity(1000);
		executor.setThreadNamePrefix("restaurant-comparison-");
		executor.initialize();
		return executor;
	}
}
