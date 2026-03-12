package com.tasteam.infra.messagequeue;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.persistence.UserActivityEventStoreService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class UserActivityMessageQueueConsumerRegistrar {

	private final MessageQueueConsumer messageQueueConsumer;
	private final MessageQueueProperties messageQueueProperties;
	private final TopicNamingPolicy topicNamingPolicy;
	private final UserActivityEventStoreService userActivityEventStoreService;
	private final ObjectMapper objectMapper;

	private MessageQueueSubscription subscription;

	@PostConstruct
	public void subscribe() {
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			log.info("메시지큐 provider가 none이라 User Activity 구독 등록을 건너뜁니다");
			return;
		}

		subscription = new MessageQueueSubscription(
			topicNamingPolicy.main(QueueTopic.USER_ACTIVITY),
			messageQueueProperties.getDefaultConsumerGroup() + "-user-activity",
			"user-activity-" + UUID.randomUUID());

		messageQueueConsumer.subscribe(subscription, message -> {
			ActivityEvent event = deserializePayload(message.payload());
			userActivityEventStoreService.store(event);
		});
	}

	@PreDestroy
	public void unsubscribe() {
		if (subscription != null) {
			messageQueueConsumer.unsubscribe(subscription);
		}
	}

	private ActivityEvent deserializePayload(JsonNode payload) {
		try {
			return objectMapper.treeToValue(payload, ActivityEvent.class);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("UserActivity 메시지 역직렬화에 실패했습니다. payload=" + payload, ex);
		}
	}
}
