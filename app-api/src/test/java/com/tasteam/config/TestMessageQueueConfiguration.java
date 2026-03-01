package com.tasteam.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.infra.messagequeue.MessageQueueConsumer;
import com.tasteam.infra.messagequeue.MessageQueueProducer;
import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.MessageQueueProviderType;

@Configuration
public class TestMessageQueueConfiguration {

	@Bean
	MessageQueueProperties messageQueueProperties() {
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		properties.setDefaultConsumerGroup("tasteam-api");
		return properties;
	}

	@Bean
	ObjectMapper objectMapper() {
		return JsonMapper.builder().findAndAddModules().build();
	}

	@Bean
	MessageQueueProducer messageQueueProducer() {
		return Mockito.mock(MessageQueueProducer.class);
	}

	@Bean
	MessageQueueConsumer messageQueueConsumer() {
		return Mockito.mock(MessageQueueConsumer.class);
	}
}
