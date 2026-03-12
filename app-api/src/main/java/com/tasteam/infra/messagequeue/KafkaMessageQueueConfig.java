package com.tasteam.infra.messagequeue;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.infra.messagequeue.serialization.JsonMessageQueueMessageSerializer;
import com.tasteam.infra.messagequeue.serialization.MessageQueueMessageSerializer;

@Configuration
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "provider", havingValue = "kafka")
@EnableConfigurationProperties({MessageQueueProperties.class, KafkaMessageQueueProperties.class})
public class KafkaMessageQueueConfig {

	@Bean
	public MessageQueueMessageSerializer messageQueueMessageSerializer(ObjectMapper objectMapper) {
		return new JsonMessageQueueMessageSerializer(objectMapper);
	}

	@Bean
	public ProducerFactory<String, String> messageQueueKafkaProducerFactory(KafkaMessageQueueProperties properties) {
		Map<String, Object> configs = new HashMap<>();
		configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
		configs.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getClientId());
		configs.put(ProducerConfig.ACKS_CONFIG, properties.getProducer().getAcks());
		configs.put(ProducerConfig.RETRIES_CONFIG, properties.getProducer().getRetries());
		configs.put(ProducerConfig.BATCH_SIZE_CONFIG, properties.getProducer().getBatchSize());
		configs.put(ProducerConfig.LINGER_MS_CONFIG, properties.getProducer().getLingerMs());
		configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		return new DefaultKafkaProducerFactory<>(configs);
	}

	@Bean
	public KafkaTemplate<String, String> messageQueueKafkaTemplate(
		ProducerFactory<String, String> messageQueueKafkaProducerFactory) {
		return new KafkaTemplate<>(messageQueueKafkaProducerFactory);
	}

	@Bean
	public ConsumerFactory<String, String> messageQueueKafkaConsumerFactory(
		MessageQueueProperties messageQueueProperties,
		KafkaMessageQueueProperties properties) {
		Map<String, Object> configs = new HashMap<>();
		configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
		configs.put(ConsumerConfig.GROUP_ID_CONFIG, messageQueueProperties.getDefaultConsumerGroup());
		configs.put(ConsumerConfig.CLIENT_ID_CONFIG, properties.getClientId());
		configs.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, properties.getConsumer().getMaxPollRecords());
		configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		return new DefaultKafkaConsumerFactory<>(configs);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> messageQueueKafkaListenerContainerFactory(
		ConsumerFactory<String, String> messageQueueKafkaConsumerFactory,
		KafkaMessageQueueProperties properties) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(messageQueueKafkaConsumerFactory);
		factory.setConcurrency(properties.getConsumer().getConcurrency());
		factory.getContainerProperties().setPollTimeout(properties.getConsumer().getPollTimeoutMillis());
		return factory;
	}
}
