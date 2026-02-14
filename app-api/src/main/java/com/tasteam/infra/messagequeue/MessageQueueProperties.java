package com.tasteam.infra.messagequeue;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tasteam.message-queue")
public class MessageQueueProperties {

	private boolean enabled = false;
	private String provider = MessageQueueProviderType.NONE.value();
	private String topicPrefix = "tasteam";
	private String defaultConsumerGroup = "tasteam-api";
	private long pollTimeoutMillis = 1000L;
	private int maxRetries = 3;

	public MessageQueueProviderType providerType() {
		return MessageQueueProviderType.from(provider);
	}

	@PostConstruct
	public void logConfiguration() {
		log.info("=== MessageQueue Configuration ===");
		log.info("enabled: {}", enabled);
		log.info("provider: {}", providerType().value());
		log.info("topicPrefix: {}", topicPrefix);
		log.info("defaultConsumerGroup: {}", defaultConsumerGroup);
		log.info("pollTimeoutMillis: {}", pollTimeoutMillis);
		log.info("maxRetries: {}", maxRetries);
		log.info("==================================");
	}
}
