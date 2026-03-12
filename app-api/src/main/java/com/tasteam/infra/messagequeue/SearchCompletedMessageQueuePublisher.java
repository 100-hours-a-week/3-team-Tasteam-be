package com.tasteam.infra.messagequeue;

import java.time.Instant;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.search.event.SearchCompletedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class SearchCompletedMessageQueuePublisher {

	private final MessageQueueProducer messageQueueProducer;
	private final MessageQueueProperties messageQueueProperties;
	private final TopicNamingPolicy topicNamingPolicy;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onSearchCompleted(SearchCompletedEvent event) {
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			return;
		}

		JsonNode payload = serializePayload(event);
		QueueMessage message = new QueueMessage(
			topicNamingPolicy.main(QueueTopic.SEARCH_COMPLETED),
			event.memberId() != null ? String.valueOf(event.memberId()) : "anonymous",
			payload,
			Map.of("eventType", "SearchCompletedEvent", "schemaVersion", "v1"),
			Instant.now(),
			null);

		messageQueueProducer.publish(message);
	}

	private JsonNode serializePayload(SearchCompletedEvent event) {
		SearchCompletedMessagePayload payload = new SearchCompletedMessagePayload(
			event.memberId(),
			event.keyword(),
			event.groupResultCount(),
			event.restaurantResultCount());
		try {
			return objectMapper.valueToTree(payload);
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("SearchCompletedEvent 메시지 직렬화에 실패했습니다", ex);
		}
	}
}
