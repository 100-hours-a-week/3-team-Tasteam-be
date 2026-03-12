package com.tasteam.infra.messagequeue;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.lang.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.infra.messagequeue.serialization.JsonQueueMessageSerializer;
import com.tasteam.infra.messagequeue.serialization.QueueMessageSerializer;
import com.tasteam.infra.messagequeue.trace.MessageQueueTraceService;

@Configuration
@EnableConfigurationProperties({MessageQueueProperties.class, KafkaMessageQueueProperties.class})
public class MessageQueueConfig {

	@Bean
	public TopicNamingPolicy topicNamingPolicy(KafkaMessageQueueProperties kafkaMessageQueueProperties) {
		return new DefaultTopicNamingPolicy(kafkaMessageQueueProperties);
	}

	@Bean
	@ConditionalOnMissingBean(QueueMessageSerializer.class)
	public QueueMessageSerializer queueMessageSerializer(ObjectMapper objectMapper) {
		return new JsonQueueMessageSerializer(objectMapper);
	}

	@Bean
	public MessageBrokerSender messageBrokerSender(MessageQueueProducer messageQueueProducer) {
		return new DefaultMessageBrokerSender(messageQueueProducer);
	}

	@Bean
	public MessageBrokerReceiver messageBrokerReceiver(MessageQueueConsumer messageQueueConsumer) {
		return new DefaultMessageBrokerReceiver(messageQueueConsumer);
	}

	@Bean
	public QueueEventPublisher queueEventPublisher(
		MessageBrokerSender messageBrokerSender,
		TopicNamingPolicy topicNamingPolicy,
		QueueMessageSerializer queueMessageSerializer) {
		return new DefaultQueueEventPublisher(messageBrokerSender, topicNamingPolicy, queueMessageSerializer);
	}

	@Bean
	public QueueEventSubscriber queueEventSubscriber(
		MessageBrokerReceiver messageBrokerReceiver,
		TopicNamingPolicy topicNamingPolicy) {
		return new DefaultQueueEventSubscriber(messageBrokerReceiver, topicNamingPolicy);
	}

	@Bean
	public MessageQueueProducer messageQueueProducer(
		MessageQueueProperties properties,
		MessageQueueTraceService traceService,
		@Nullable
		StringRedisTemplate stringRedisTemplate) {
		MessageQueueProviderType providerType = properties.effectiveProviderType();
		MessageQueueProducer delegate = switch (providerType) {
			case NONE -> new NoOpMessageQueueProducer();
			case REDIS_STREAM ->
				new RedisStreamMessageQueueProducer(requireRedisTemplate(stringRedisTemplate), properties);
			case KAFKA -> new UnsupportedMessageQueueProducer(providerType);
		};
		return new TracingMessageQueueProducer(delegate, providerType, traceService);
	}

	@Bean
	public MessageQueueConsumer messageQueueConsumer(
		MessageQueueProperties properties,
		MessageQueueTraceService traceService,
		ObjectMapper objectMapper,
		@Nullable
		StringRedisTemplate stringRedisTemplate,
		@Nullable @Qualifier("messageQueueStreamListenerContainer")
		StreamMessageListenerContainer<String, MapRecord<String, String, String>> messageQueueStreamListenerContainer) {
		MessageQueueProviderType providerType = properties.effectiveProviderType();
		MessageQueueConsumer delegate = switch (providerType) {
			case NONE -> new NoOpMessageQueueConsumer();
			case REDIS_STREAM -> new RedisStreamMessageQueueConsumer(
				requireRedisTemplate(stringRedisTemplate),
				requireStreamListenerContainer(messageQueueStreamListenerContainer),
				properties,
				objectMapper);
			case KAFKA -> new UnsupportedMessageQueueConsumer(providerType);
		};
		return new TracingMessageQueueConsumer(delegate, providerType, traceService);
	}

	private StringRedisTemplate requireRedisTemplate(@Nullable
	StringRedisTemplate stringRedisTemplate) {
		if (stringRedisTemplate == null) {
			throw new IllegalStateException("redis-stream provider는 StringRedisTemplate 빈이 필요합니다");
		}
		return stringRedisTemplate;
	}

	private StreamMessageListenerContainer<String, MapRecord<String, String, String>> requireStreamListenerContainer(
		@Nullable
		StreamMessageListenerContainer<String, MapRecord<String, String, String>> messageQueueStreamListenerContainer) {
		if (messageQueueStreamListenerContainer == null) {
			throw new IllegalStateException("redis-stream provider는 StreamMessageListenerContainer 빈이 필요합니다");
		}
		return messageQueueStreamListenerContainer;
	}
}
