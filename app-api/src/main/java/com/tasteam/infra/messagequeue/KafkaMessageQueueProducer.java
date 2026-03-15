package com.tasteam.infra.messagequeue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.kafka.core.KafkaTemplate;

import com.tasteam.infra.messagequeue.exception.MessageQueuePublishException;
import com.tasteam.infra.messagequeue.serialization.QueueMessageSerializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KafkaMessageQueueProducer implements MessageQueueProducer {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final KafkaMessageQueueProperties kafkaProperties;
	private final QueueMessageSerializer serializer;

	@Override
	public void publish(QueueMessage message) {
		String serialized = serializer.serialize(message);
		try {
			kafkaTemplate.executeInTransaction(ops -> {
				try {
					return ops.send(message.topic(), message.key(), serialized)
						.get(kafkaProperties.getProducer().getSendTimeoutMillis(), TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				} catch (ExecutionException | TimeoutException e) {
					throw new RuntimeException(e);
				}
			});
			log.debug("Kafka publish 성공. topic={}, messageId={}", message.topic(), message.messageId());
		} catch (Exception ex) {
			log.error("Kafka publish 실패. topic={}, messageId={}", message.topic(), message.messageId(), ex);
			throw new MessageQueuePublishException(message.topic(), message.messageId(), ex);
		}
	}
}
