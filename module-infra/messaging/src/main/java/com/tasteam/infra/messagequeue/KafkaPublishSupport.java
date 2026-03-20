package com.tasteam.infra.messagequeue;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.tasteam.infra.messagequeue.exception.MessageQueuePublishException;
import com.tasteam.infra.messagequeue.serialization.QueueMessageSerializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KafkaPublishSupport {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final KafkaMessageQueueProperties kafkaProperties;
	private final QueueMessageSerializer queueMessageSerializer;

	public void publish(String topic, String key, Object payload, Map<String, String> headers) {
		QueueMessage message = queueMessageSerializer.createMessage(topic, key, payload, headers);
		publish(message);
	}

	public void publish(String topic, String key, Object payload) {
		publish(topic, key, payload, Map.of());
	}

	public void publish(QueueMessage message) {
		String serializedMessage = queueMessageSerializer.serialize(message);
		try {
			SendResult<String, String> result = kafkaTemplate.send(message.topic(), message.key(), serializedMessage)
				.get(kafkaProperties.getProducer().getSendTimeoutMillis(), TimeUnit.MILLISECONDS);
			if (result != null && result.getRecordMetadata() != null) {
				log.debug("Kafka publish success. topic={}, partition={}, offset={}, messageId={}",
					message.topic(),
					result.getRecordMetadata().partition(),
					result.getRecordMetadata().offset(),
					message.messageId());
			} else {
				log.debug("Kafka publish success. topic={}, messageId={}", message.topic(), message.messageId());
			}
		} catch (Exception ex) {
			log.error("Kafka publish failed. topic={}, key={}, messageId={}",
				message.topic(), message.key(), message.messageId(), ex);
			throw new MessageQueuePublishException(message.topic(), message.messageId(), ex);
		}
	}
}
