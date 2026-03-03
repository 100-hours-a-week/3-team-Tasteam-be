package com.tasteam.global.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

	private final RedisProperties redisProperties;

	@PostConstruct
	public void validateAndLogRedisConfig() {
		List<String> missingConfigs = new ArrayList<>();

		log.info("=== Redis Configuration ===");
		log.info("Enabled: {}", redisProperties.isEnabled());

		if (redisProperties.getHost() == null || redisProperties.getHost().isBlank()) {
			missingConfigs.add("spring.data.redis.host");
			log.warn("Redis host is not configured");
		} else {
			log.info("Host: {}", redisProperties.getHost());
		}

		log.info("Port: {}", redisProperties.getPort());
		log.info("Database: {}", redisProperties.getDatabase());

		if (redisProperties.getPassword() != null && !redisProperties.getPassword().isBlank()) {
			log.info("Password: ****");
		} else {
			log.info("Password: not set");
		}

		if (!missingConfigs.isEmpty()) {
			log.error("Missing required Redis configurations: {}", String.join(", ", missingConfigs));
		}

		log.info("===========================");
	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
		return template;
	}
}
