package com.tasteam.infra.messagequeue;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MessageQueueProperties.class)
public class MessageQueueConfig {

	@Bean
	public MessageQueueProducer messageQueueProducer(MessageQueueProperties properties) {
		MessageQueueProviderType providerType = properties.providerType();
		return switch (providerType) {
			case NONE -> new NoOpMessageQueueProducer();
			case REDIS_STREAM, KAFKA -> new UnsupportedMessageQueueProducer(providerType);
		};
	}

	@Bean
	public MessageQueueConsumer messageQueueConsumer(MessageQueueProperties properties) {
		MessageQueueProviderType providerType = properties.providerType();
		return switch (providerType) {
			case NONE -> new NoOpMessageQueueConsumer();
			case REDIS_STREAM, KAFKA -> new UnsupportedMessageQueueConsumer(providerType);
		};
	}
}
