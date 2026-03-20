package com.tasteam.infra.messagequeue;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.ContainerProperties.AckMode;

import com.tasteam.infra.messagequeue.serialization.QueueMessageSerializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KafkaMessageQueueConsumer implements MessageQueueConsumer {

	private final ConcurrentKafkaListenerContainerFactory<String, String> containerFactory;
	private final QueueMessageSerializer serializer;
	private final CommonErrorHandler errorHandler;

	private final Map<String, ConcurrentMessageListenerContainer<String, String>> containers = new ConcurrentHashMap<>();

	@Override
	public void subscribe(MessageQueueSubscription subscription, QueueMessageHandler handler) {
		containers.computeIfAbsent(subscription.consumerName(), name -> {
			ContainerProperties props = new ContainerProperties(subscription.topic());
			props.setGroupId(subscription.consumerGroup());
			props.setAckMode(AckMode.MANUAL_IMMEDIATE);
			props.setMessageListener(
				(AcknowledgingMessageListener<String, String>)(record, ack) -> {
					QueueMessage message = serializer.deserialize(record.value());
					handler.handle(message);
					ack.acknowledge();
				});
			props.setConsumerRebalanceListener(new ConsumerAwareRebalanceListener() {
				@Override
				public void onPartitionsRevokedBeforeCommit(Consumer<?, ?> consumer,
					Collection<TopicPartition> partitions) {
					log.info("Kafka 파티션 반환 시작 (커밋 전). topic={}, partitions={}",
						subscription.topic(), partitions);
				}

				@Override
				public void onPartitionsAssigned(Consumer<?, ?> consumer,
					Collection<TopicPartition> partitions) {
					log.info("Kafka 파티션 배정 완료. topic={}, partitions={}",
						subscription.topic(), partitions);
				}
			});
			ConcurrentMessageListenerContainer<String, String> container = new ConcurrentMessageListenerContainer<>(
				containerFactory.getConsumerFactory(), props);
			container.setCommonErrorHandler(errorHandler);
			container.start();
			log.info("Kafka 구독 컨테이너 시작. topic={}, group={}, consumer={}",
				subscription.topic(), subscription.consumerGroup(), subscription.consumerName());
			return container;
		});
	}

	@Override
	public void unsubscribe(MessageQueueSubscription subscription) {
		ConcurrentMessageListenerContainer<String, String> container = containers.remove(subscription.consumerName());
		if (container != null) {
			container.stop();
			log.info("Kafka 구독 컨테이너 종료. consumer={}", subscription.consumerName());
		}
	}
}
