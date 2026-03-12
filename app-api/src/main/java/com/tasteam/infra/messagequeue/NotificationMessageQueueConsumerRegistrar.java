package com.tasteam.infra.messagequeue;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.NotificationService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class NotificationMessageQueueConsumerRegistrar {

	private final QueueEventSubscriber queueEventSubscriber;
	private final MessageQueueProperties messageQueueProperties;
	private final NotificationService notificationService;
	private final ObjectMapper objectMapper;

	private MessageQueueSubscription subscription;

	@PostConstruct
	public void subscribe() {
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			log.info("메시지큐 provider가 none이라 Notification 구독 등록을 건너뜁니다");
			return;
		}

		subscription = queueEventSubscriber.subscribe(QueueTopic.GROUP_MEMBER_JOINED, message -> {
			GroupMemberJoinedMessagePayload payload = deserializePayload(message.payload());
			notificationService.createNotification(
				payload.memberId(),
				NotificationType.SYSTEM,
				"그룹 가입 완료",
				payload.groupName() + " 그룹에 가입되었습니다.",
				"/groups/" + payload.groupId());
		});
	}

	@PreDestroy
	public void unsubscribe() {
		if (subscription != null) {
			queueEventSubscriber.unsubscribe(subscription);
		}
	}

	private GroupMemberJoinedMessagePayload deserializePayload(JsonNode payload) {
		try {
			return objectMapper.treeToValue(payload, GroupMemberJoinedMessagePayload.class);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("GroupMemberJoined 메시지 역직렬화에 실패했습니다. payload=" + payload, ex);
		}
	}

}
