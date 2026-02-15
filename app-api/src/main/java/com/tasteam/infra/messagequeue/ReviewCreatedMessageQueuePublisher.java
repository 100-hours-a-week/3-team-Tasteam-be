package com.tasteam.infra.messagequeue;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.review.event.ReviewCreatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class ReviewCreatedMessageQueuePublisher {

	private final MessageQueueProducer messageQueueProducer;
	private final MessageQueueProperties messageQueueProperties;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onReviewCreated(ReviewCreatedEvent event) {
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			return;
		}

		byte[] payload = serializePayload(event.restaurantId());
		MessageQueueMessage message = new MessageQueueMessage(
			MessageQueueTopics.REVIEW_CREATED,
			String.valueOf(event.restaurantId()),
			payload,
			Map.of("eventType", "ReviewCreatedEvent", "schemaVersion", "v1"),
			null,
			null);

		messageQueueProducer.publish(message);
	}

	private byte[] serializePayload(long restaurantId) {
		try {
			return objectMapper.writeValueAsBytes(new ReviewCreatedMessagePayload(restaurantId));
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("ReviewCreatedEvent 메시지 직렬화에 실패했습니다", ex);
		}
	}
}
