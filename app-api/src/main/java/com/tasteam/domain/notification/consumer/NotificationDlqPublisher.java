package com.tasteam.domain.notification.consumer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.tasteam.global.aop.ObservedMqDlq;
import com.tasteam.infra.messagequeue.MessageQueueProducer;
import com.tasteam.infra.messagequeue.MessageQueueTopics;
import com.tasteam.infra.messagequeue.QueueMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class NotificationDlqPublisher {

	private final MessageQueueProducer messageQueueProducer;

	@ObservedMqDlq(topic = MessageQueueTopics.NOTIFICATION_REQUESTED_DLQ)
	public void publish(QueueMessage message) {
		QueueMessage dlqMessage = new QueueMessage(
			MessageQueueTopics.NOTIFICATION_REQUESTED_DLQ,
			message.key(),
			message.payload(),
			message.headers(),
			message.occurredAt(),
			message.messageId());
		messageQueueProducer.publish(dlqMessage);
		log.info("알림 DLQ 발행 완료. messageId={}", message.messageId());
	}
}
