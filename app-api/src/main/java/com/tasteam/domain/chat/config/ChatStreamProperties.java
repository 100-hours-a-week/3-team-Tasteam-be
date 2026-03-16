package com.tasteam.domain.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat.stream")
public record ChatStreamProperties(
	boolean enabled,
	boolean bootstrapEnabled,
	// Legacy room consume path only.
	int bootstrapBatchSize,
	// Legacy room consume path only.
	int maxTotalSubscriptions,
	int executorThreadPoolSize,
	int executorQueueCapacity,
	int partitionCount,
	int maxAllowedPartitions,
	boolean partitionConsumeEnabled,
	boolean dualWriteEnabled,
	boolean legacyRoomConsumeEnabled,
	boolean wsPubSubBroadcastEnabled,
	String wsPubSubChannel) {
	public ChatStreamProperties() {
		this(
			true,
			true,
			100,
			1000,
			4,
			256,
			16,
			128,
			true,
			true,
			false,
			false,
			"chat:websocket:broadcast");
	}
}
