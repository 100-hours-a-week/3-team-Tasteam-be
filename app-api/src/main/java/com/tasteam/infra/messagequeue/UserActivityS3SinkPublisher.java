package com.tasteam.infra.messagequeue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.api.ActivitySink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ActivityEvent}를 S3 적재 전용 Kafka 토픽({@code evt.user-activity.s3-ingest.v1})으로 발행하는 sink.
 *
 * <p>Kafka Connect S3 Sink Connector가 해당 토픽을 구독하여
 * {@code raw/events/dt=YYYY-MM-DD/} 경로에 CSV로 적재한다.
 * outbox 없이 fire-and-forget 방식으로 발행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class UserActivityS3SinkPublisher implements ActivitySink {

	private final MessageQueueProducer messageQueueProducer;
	private final MessageQueueProperties messageQueueProperties;
	private final TopicNamingPolicy topicNamingPolicy;
	private final ObjectMapper objectMapper;

	@Override
	public String sinkType() {
		return "USER_ACTIVITY_S3_INGEST";
	}

	@Override
	public void sink(ActivityEvent event) {
		Objects.requireNonNull(event, "event는 null일 수 없습니다.");
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			log.debug("메시지큐 provider가 none이라 S3 sink 발행을 건너뜁니다. eventId={}", event.eventId());
			return;
		}

		UserActivityS3Event s3Event = toS3Event(event);
		QueueMessage message = new QueueMessage(
			topicNamingPolicy.main(QueueTopic.USER_ACTIVITY_S3_INGEST),
			resolveMessageKey(event),
			serialize(s3Event),
			buildHeaders(event),
			event.occurredAt(),
			event.eventId());
		messageQueueProducer.publish(message);
	}

	private UserActivityS3Event toS3Event(ActivityEvent event) {
		Map<String, Object> props = event.properties();
		String diningType = stringOrNull(props, "diningType");
		String distanceBucket = stringOrNull(props, "distanceBucket");
		String weatherBucket = stringOrNull(props, "weatherBucket");
		String sessionId = stringOrNull(props, "sessionId");
		Long restaurantId = longOrNull(props, "restaurantId");
		String recommendationId = stringOrNull(props, "recommendationId");
		String platform = stringOrNull(props, "platform");
		Instant createdAt = Instant.now();

		return new UserActivityS3Event(
			event.eventId(),
			event.eventName(),
			event.eventVersion(),
			event.occurredAt(),
			diningType,
			distanceBucket,
			weatherBucket,
			event.memberId(),
			event.anonymousId(),
			sessionId,
			restaurantId,
			recommendationId,
			platform,
			createdAt);
	}

	private String resolveMessageKey(ActivityEvent event) {
		if (event.memberId() != null) {
			return String.valueOf(event.memberId());
		}
		if (event.anonymousId() != null && !event.anonymousId().isBlank()) {
			return event.anonymousId();
		}
		return null;
	}

	private Map<String, String> buildHeaders(ActivityEvent event) {
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("eventType", "UserActivityS3Event");
		headers.put("eventName", event.eventName());
		headers.put("schemaVersion", event.eventVersion());
		return Map.copyOf(headers);
	}

	private JsonNode serialize(UserActivityS3Event s3Event) {
		return objectMapper.valueToTree(s3Event);
	}

	private static String stringOrNull(Map<String, Object> props, String key) {
		Object value = props.get(key);
		if (value == null) {
			return null;
		}
		String str = value.toString().strip();
		return str.isEmpty() ? null : str;
	}

	private static Long longOrNull(Map<String, Object> props, String key) {
		Object value = props.get(key);
		if (value == null) {
			return null;
		}
		if (value instanceof Long l) {
			return l;
		}
		if (value instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(value.toString().strip());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
