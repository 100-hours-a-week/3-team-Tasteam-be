package com.tasteam.infra.messagequeue;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

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
			kafkaTemplate.send(message.topic(), message.key(), serialized)
				.whenComplete((result, ex) -> handlePublishResult(message, result, ex));
			log.debug("Kafka publish 요청 전송. topic={}, messageId={}", message.topic(), message.messageId());
		} catch (Exception ex) {
			log.error("Kafka publish 실패. topic={}, messageId={}", message.topic(), message.messageId(), ex);
			throw new MessageQueuePublishException(message.topic(), message.messageId(), ex);
		}
	}

	private void handlePublishResult(QueueMessage message, SendResult<String, String> result, Throwable ex) {
		if (ex != null) {
			log.error("Kafka publish 비동기 실패. topic={}, messageId={}", message.topic(), message.messageId(), ex);
			return;
		}
		if (result != null && result.getRecordMetadata() != null) {
			log.debug("Kafka publish 성공. topic={}, partition={}, offset={}, messageId={}",
				message.topic(),
				result.getRecordMetadata().partition(),
				result.getRecordMetadata().offset(),
				message.messageId());
			return;
		}
		log.debug("Kafka publish 성공. topic={}, messageId={}", message.topic(), message.messageId());
	}
}
