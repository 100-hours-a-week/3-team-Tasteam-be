package com.tasteam.domain.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.stream")
public record ChatStreamProperties(
	boolean enabled,
	boolean bootstrapEnabled,
	int bootstrapBatchSize,
	int maxTotalSubscriptions,
	int executorThreadPoolSize,
	int executorQueueCapacity) {
	public ChatStreamProperties() {
		this(
			true,
			true,
			100,
			1000,
			4,
			256);
	}
}
