package com.tasteam.domain.notification.consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.tasteam.global.aop.ObservedMqDlq;
import com.tasteam.infra.messagequeue.MessageQueueProducer;
import com.tasteam.infra.messagequeue.QueueMessage;
import com.tasteam.infra.messagequeue.QueueTopic;
import com.tasteam.infra.messagequeue.TopicNamingPolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class NotificationDlqPublisher {

	private static final String OBSERVED_DLQ_TOPIC = "evt.notification.v1.dlq";

	private final MessageQueueProducer messageQueueProducer;
	private final TopicNamingPolicy topicNamingPolicy;

	@ObservedMqDlq(topic = OBSERVED_DLQ_TOPIC)
	public void publish(QueueMessage message) {
		QueueMessage dlqMessage = new QueueMessage(
			topicNamingPolicy.dlq(QueueTopic.NOTIFICATION_REQUESTED),
			message.key(),
			message.payload(),
			message.headers(),
			message.occurredAt(),
			message.messageId());
		messageQueueProducer.publish(dlqMessage);
		log.info("알림 DLQ 발행 완료. messageId={}", message.messageId());
	}
}
