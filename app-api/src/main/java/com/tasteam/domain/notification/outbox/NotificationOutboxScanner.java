package com.tasteam.domain.notification.outbox;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;
import com.tasteam.global.aop.ObservedAsyncPipeline;
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
public class NotificationOutboxScanner {

	@Value("${tasteam.notification.outbox.batch-size:100}")
	private int batchSize;

	private final NotificationOutboxService outboxService;
	private final MessageQueueProducer messageQueueProducer;
	private final TopicNamingPolicy topicNamingPolicy;
	private final ObjectMapper objectMapper;

	@ObservedAsyncPipeline(domain = "notification", stage = "outbox_scan")
	@Scheduled(fixedDelayString = "${tasteam.notification.outbox.scan-delay:30000}")
	public void scan() {
		List<NotificationRequestedPayload> candidates = outboxService.findCandidates(batchSize);
		if (candidates.isEmpty()) {
			return;
		}
		log.info("알림 아웃박스 스캐너 실행. count={}", candidates.size());
		for (NotificationRequestedPayload payload : candidates) {
			try {
				byte[] payloadBytes = objectMapper.writeValueAsBytes(payload);
				QueueMessage message = QueueMessage.of(
					topicNamingPolicy.main(QueueTopic.NOTIFICATION_REQUESTED),
					payload.recipientId().toString(),
					payloadBytes);
				messageQueueProducer.publish(message);
				outboxService.markPublished(payload.eventId());
			} catch (JsonProcessingException ex) {
				log.error("알림 아웃박스 payload 직렬화 실패. eventId={}", payload.eventId(), ex);
				outboxService.markFailed(payload.eventId(), ex.getMessage());
			} catch (Exception ex) {
				log.error("알림 아웃박스 발행 실패. eventId={}", payload.eventId(), ex);
				outboxService.markFailed(payload.eventId(), ex.getMessage());
			}
		}
	}
}
