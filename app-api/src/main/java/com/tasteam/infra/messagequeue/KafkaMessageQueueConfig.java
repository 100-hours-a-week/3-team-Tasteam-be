package com.tasteam.infra.messagequeue;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.infra.messagequeue.exception.MessageQueueNonRetryableException;
import com.tasteam.infra.messagequeue.serialization.JsonQueueMessageSerializer;
import com.tasteam.infra.messagequeue.serialization.QueueMessageSerializer;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "provider", havingValue = "kafka")
@EnableConfigurationProperties({MessageQueueProperties.class, KafkaMessageQueueProperties.class})
public class KafkaMessageQueueConfig {

	@Bean
	@ConditionalOnMissingBean(QueueMessageSerializer.class)
	public QueueMessageSerializer queueMessageSerializer(ObjectMapper objectMapper) {
		return new JsonQueueMessageSerializer(objectMapper);
	}

	@Bean
	@ConditionalOnMissingBean(TopicNamingPolicy.class)
	public TopicNamingPolicy topicNamingPolicy(KafkaMessageQueueProperties kafkaProperties) {
		return new DefaultTopicNamingPolicy(kafkaProperties);
	}

	@Bean
	public KafkaPublishSupport kafkaPublishSupport(
		KafkaTemplate<String, String> messageQueueKafkaTemplate,
		KafkaMessageQueueProperties kafkaProperties,
		QueueMessageSerializer queueMessageSerializer) {
		return new KafkaPublishSupport(messageQueueKafkaTemplate, kafkaProperties, queueMessageSerializer);
	}

	@Bean
	public DeadLetterPublishingRecoverer messageQueueDeadLetterPublishingRecoverer(
		KafkaTemplate<String, String> messageQueueKafkaTemplate,
		TopicNamingPolicy topicNamingPolicy,
		MeterRegistry meterRegistry) {
		return new DeadLetterPublishingRecoverer(
			messageQueueKafkaTemplate,
			(record, exception) -> new TopicPartition(
				topicNamingPolicy.dlq(record.topic()),
				record.partition())) {

			@Override
			public void accept(org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record, Exception ex) {
				super.accept(record, ex);
				meterRegistry.counter("notification.consumer.dlq",
					"topic", record.topic(), "result", "dlt").increment();
				log.warn("알림 Kafka DLT 발행. topic={}, key={}", record.topic(), record.key(), ex);
			}
		};
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
		configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
		configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(configs);
		factory.setTransactionIdPrefix(properties.getProducer().getTransactionIdPrefix());
		return factory;
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
		configs.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
		configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		return new DefaultKafkaConsumerFactory<>(configs);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> messageQueueKafkaListenerContainerFactory(
		ConsumerFactory<String, String> messageQueueKafkaConsumerFactory,
		CommonErrorHandler messageQueueKafkaErrorHandler,
		KafkaMessageQueueProperties properties) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(messageQueueKafkaConsumerFactory);
		factory.setConcurrency(properties.getConsumer().getConcurrency());
		factory.getContainerProperties().setPollTimeout(properties.getConsumer().getPollTimeoutMillis());
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
		factory.setCommonErrorHandler(messageQueueKafkaErrorHandler);
		return factory;
	}

	@Bean("kafkaMessageQueueProducerDelegate")
	public MessageQueueProducer kafkaMessageQueueProducer(
		KafkaTemplate<String, String> messageQueueKafkaTemplate,
		KafkaMessageQueueProperties kafkaProperties,
		QueueMessageSerializer queueMessageSerializer) {
		return new KafkaMessageQueueProducer(messageQueueKafkaTemplate, kafkaProperties, queueMessageSerializer);
	}

	@Bean("kafkaMessageQueueConsumerDelegate")
	public MessageQueueConsumer kafkaMessageQueueConsumer(
		@Qualifier("messageQueueKafkaListenerContainerFactory")
		ConcurrentKafkaListenerContainerFactory<String, String> containerFactory,
		QueueMessageSerializer queueMessageSerializer,
		CommonErrorHandler messageQueueKafkaErrorHandler) {
		return new KafkaMessageQueueConsumer(containerFactory, queueMessageSerializer, messageQueueKafkaErrorHandler);
	}

	@Bean
	public CommonErrorHandler messageQueueKafkaErrorHandler(
		KafkaMessageQueueProperties properties,
		DeadLetterPublishingRecoverer messageQueueDeadLetterPublishingRecoverer) {
		long maxAttempts = Math.max(1, properties.getConsumer().getRetry().getMaxAttempts());
		long backoffMillis = Math.max(0, properties.getConsumer().getRetry().getBackoffMillis());

		DefaultErrorHandler errorHandler = new DefaultErrorHandler(
			messageQueueDeadLetterPublishingRecoverer,
			new FixedBackOff(backoffMillis, maxAttempts - 1));
		errorHandler.addNotRetryableExceptions(
			MessageQueueNonRetryableException.class,
			IllegalArgumentException.class,
			DeserializationException.class,
			org.apache.kafka.common.errors.SerializationException.class);
		return errorHandler;
	}
}
