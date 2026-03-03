package com.tasteam.domain.notification.consumer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.notification.dispatch.NotificationDispatcher;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;
import com.tasteam.infra.messagequeue.MessageQueueConsumer;
import com.tasteam.infra.messagequeue.MessageQueueMessage;
import com.tasteam.infra.messagequeue.MessageQueueProducer;
import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.MessageQueueProviderType;
import com.tasteam.infra.messagequeue.MessageQueueSubscription;
import com.tasteam.infra.messagequeue.MessageQueueTopics;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class NotificationMessageQueueConsumer {

	private static final String CONSUMER_GROUP = "cg.notification.processor.v1";

	@Value("${tasteam.message-queue.max-retries:3}")
	private int maxRetries;

	private final Map<String, Integer> retryCountMap = new ConcurrentHashMap<>();

	private final MessageQueueConsumer messageQueueConsumer;
	private final MessageQueueProperties messageQueueProperties;
	private final MessageQueueProducer messageQueueProducer;
	private final NotificationDispatcher dispatcher;
	private final ObjectMapper objectMapper;

	private MessageQueueSubscription subscription;

	@PostConstruct
	public void subscribe() {
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			log.info("메시지큐 provider가 none이라 알림 구독 등록을 건너뜁니다");
			return;
		}

		subscription = new MessageQueueSubscription(
			MessageQueueTopics.NOTIFICATION_REQUESTED,
			CONSUMER_GROUP,
			"notification-processor-" + UUID.randomUUID());

		messageQueueConsumer.subscribe(subscription, this::handleMessage);
		log.info("알림 메시지큐 구독 등록 완료. topic={}, consumerGroup={}", MessageQueueTopics.NOTIFICATION_REQUESTED,
			CONSUMER_GROUP);
	}

	@PreDestroy
	public void unsubscribe() {
		if (subscription != null) {
			messageQueueConsumer.unsubscribe(subscription);
		}
	}

	private void handleMessage(MessageQueueMessage message) {
		NotificationRequestedPayload payload = deserializePayload(message.payload());
		try {
			dispatcher.dispatch(payload);
			retryCountMap.remove(message.messageId());
		} catch (Exception ex) {
			int count = retryCountMap.merge(message.messageId(), 1, Integer::sum);
			log.warn("알림 처리 실패. eventId={}, retryCount={}/{}", payload.eventId(), count, maxRetries, ex);
			if (count >= maxRetries) {
				publishToDlq(message);
				retryCountMap.remove(message.messageId());
			} else {
				throw ex;
			}
		}
	}

	private void publishToDlq(MessageQueueMessage message) {
		try {
			MessageQueueMessage dlqMessage = MessageQueueMessage.of(
				MessageQueueTopics.NOTIFICATION_REQUESTED_DLQ,
				message.key(),
				message.payload());
			messageQueueProducer.publish(dlqMessage);
			log.info("알림 DLQ 발행 완료. messageId={}", message.messageId());
		} catch (Exception ex) {
			log.error("알림 DLQ 발행 실패. messageId={}", message.messageId(), ex);
		}
	}

	private NotificationRequestedPayload deserializePayload(byte[] payload) {
		try {
			return objectMapper.readValue(payload, NotificationRequestedPayload.class);
		} catch (IOException ex) {
			String payloadAsString = new String(payload, StandardCharsets.UTF_8);
			throw new IllegalArgumentException("알림 메시지 역직렬화 실패. payload=" + payloadAsString, ex);
		}
	}
}
