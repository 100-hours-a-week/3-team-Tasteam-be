package com.tasteam.domain.chat.stream;

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
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ChatStreamConfig {

	@Bean
	public StreamMessageListenerContainer<String, MapRecord<String, String, String>> chatStreamListenerContainer(
		RedisConnectionFactory connectionFactory) {
		StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options = StreamMessageListenerContainerOptions
			.<String, MapRecord<String, String, String>>builder()
			.pollTimeout(Duration.ofSeconds(1))
			.batchSize(10)
			.serializer(new StringRedisSerializer())
			.build();

		return StreamMessageListenerContainer.create(connectionFactory, options);
	}
}
