package com.tasteam.domain.chat.stream;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.tasteam.domain.chat.config.ChatStreamProperties;

@Configuration
@EnableConfigurationProperties(ChatStreamProperties.class)
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ChatStreamConfig {

	@Bean(destroyMethod = "shutdown")
	public ThreadPoolTaskExecutor chatStreamExecutor(ChatStreamProperties properties) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(properties.executorThreadPoolSize());
		executor.setMaxPoolSize(properties.executorThreadPoolSize());
		executor.setQueueCapacity(properties.executorQueueCapacity());
		executor.setThreadNamePrefix("chat-stream-");
		executor.initialize();
		return executor;
	}

	@Bean
	public StreamMessageListenerContainer<String, MapRecord<String, String, String>> chatStreamListenerContainer(
		RedisConnectionFactory connectionFactory,
		ThreadPoolTaskExecutor chatStreamExecutor) {
		StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options = StreamMessageListenerContainerOptions
			.<String, MapRecord<String, String, String>>builder()
			.pollTimeout(Duration.ofSeconds(1))
			.batchSize(10)
			.serializer(new StringRedisSerializer())
			.executor(chatStreamExecutor)
			.build();

		return StreamMessageListenerContainer.create(connectionFactory, options);
	}
}
