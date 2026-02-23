package com.tasteam.batch.ai.vector.config;

import java.util.concurrent.Executor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(VectorUploadBatchProperties.class)
public class VectorUploadBatchConfig {

	@Bean(name = "vectorUploadExecutor")
	public Executor vectorUploadExecutor(VectorUploadBatchProperties properties) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(properties.getWorkerPoolSize());
		executor.setMaxPoolSize(properties.getWorkerPoolSize());
		executor.setQueueCapacity(0);
		executor.setThreadNamePrefix("vector-upload-");
		executor.initialize();
		return executor;
	}
}
