package com.tasteam.infra.messagequeue;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

@Configuration
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "provider", havingValue = "redis-stream")
public class MessageQueueStreamContainerConfig {

	@Bean(destroyMethod = "stop")
	public StreamMessageListenerContainer<String, MapRecord<String, String, String>> messageQueueStreamListenerContainer(
		RedisConnectionFactory redisConnectionFactory,
		MessageQueueProperties properties) {
		StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options = StreamMessageListenerContainerOptions
			.<String, MapRecord<String, String, String>>builder()
			.pollTimeout(Duration.ofMillis(properties.getPollTimeoutMillis()))
			.batchSize(10)
			.serializer(new StringRedisSerializer())
			.build();
		return StreamMessageListenerContainer.create(redisConnectionFactory, options);
	}
}
