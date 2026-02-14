package com.tasteam.infra.messagequeue;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.lang.Nullable;

@Configuration
@EnableConfigurationProperties(MessageQueueProperties.class)
public class MessageQueueConfig {

	@Bean
	public MessageQueueProducer messageQueueProducer(
		MessageQueueProperties properties,
		@Nullable
		StringRedisTemplate stringRedisTemplate) {
		MessageQueueProviderType providerType = properties.providerType();
		return switch (providerType) {
			case NONE -> new NoOpMessageQueueProducer();
			case REDIS_STREAM ->
				new RedisStreamMessageQueueProducer(requireRedisTemplate(stringRedisTemplate), properties);
			case KAFKA -> new UnsupportedMessageQueueProducer(providerType);
		};
	}

	@Bean
	public MessageQueueConsumer messageQueueConsumer(
		MessageQueueProperties properties,
		@Nullable
		StringRedisTemplate stringRedisTemplate,
		@Nullable
		StreamMessageListenerContainer<String, MapRecord<String, String, String>> messageQueueStreamListenerContainer) {
		MessageQueueProviderType providerType = properties.providerType();
		return switch (providerType) {
			case NONE -> new NoOpMessageQueueConsumer();
			case REDIS_STREAM -> new RedisStreamMessageQueueConsumer(
				requireRedisTemplate(stringRedisTemplate),
				requireStreamListenerContainer(messageQueueStreamListenerContainer),
				properties);
			case KAFKA -> new UnsupportedMessageQueueConsumer(providerType);
		};
	}

	private StringRedisTemplate requireRedisTemplate(@Nullable
	StringRedisTemplate stringRedisTemplate) {
		if (stringRedisTemplate == null) {
			throw new IllegalStateException("redis-stream provider requires StringRedisTemplate bean");
		}
		return stringRedisTemplate;
	}

	private StreamMessageListenerContainer<String, MapRecord<String, String, String>> requireStreamListenerContainer(
		@Nullable
		StreamMessageListenerContainer<String, MapRecord<String, String, String>> messageQueueStreamListenerContainer) {
		if (messageQueueStreamListenerContainer == null) {
			throw new IllegalStateException("redis-stream provider requires StreamMessageListenerContainer bean");
		}
		return messageQueueStreamListenerContainer;
	}
}
