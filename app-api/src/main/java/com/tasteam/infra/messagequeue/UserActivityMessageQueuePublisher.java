package com.tasteam.infra.messagequeue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.api.ActivitySink;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class UserActivityMessageQueuePublisher implements ActivitySink {

	private final MessageQueueProducer messageQueueProducer;
	private final MessageQueueProperties messageQueueProperties;
	private final ObjectMapper objectMapper;
	private final UserActivitySourceOutboxService outboxService;

	@Override
	public String sinkType() {
		return "USER_ACTIVITY_MQ";
	}

	@Override
	public void sink(ActivityEvent event) {
		Objects.requireNonNull(event, "event는 null일 수 없습니다.");
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			log.info("메시지큐 provider가 none이라 User Activity 발행을 건너뜁니다. eventId={}", event.eventId());
			return;
		}

		try {
			MessageQueueMessage message = new MessageQueueMessage(
				MessageQueueTopics.USER_ACTIVITY,
				resolveMessageKey(event),
				serialize(event),
				buildHeaders(event),
				event.occurredAt(),
				event.eventId());
			messageQueueProducer.publish(message);
			outboxService.markPublished(event.eventId());
		} catch (Exception ex) {
			outboxService.markFailed(event.eventId(), ex);
			throw ex;
		}
	}

	private byte[] serialize(ActivityEvent event) {
		try {
			return objectMapper.writeValueAsBytes(event);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("사용자 이벤트 메시지 직렬화에 실패했습니다", ex);
		}
	}

	private Map<String, String> buildHeaders(ActivityEvent event) {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("eventType", "ActivityEvent");
		headers.put("eventName", event.eventName());
		headers.put("schemaVersion", event.eventVersion());
		return Map.copyOf(headers);
	}

	private String resolveMessageKey(ActivityEvent event) {
		if (event.memberId() != null) {
			return String.valueOf(event.memberId());
		}
		if (event.anonymousId() != null && !event.anonymousId().isBlank()) {
			return event.anonymousId();
		}
		return event.eventId();
	}
}
