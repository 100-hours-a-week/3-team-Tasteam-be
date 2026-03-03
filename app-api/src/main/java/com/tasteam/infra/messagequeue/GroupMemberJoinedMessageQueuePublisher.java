package com.tasteam.infra.messagequeue;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.group.event.GroupMemberJoinedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class GroupMemberJoinedMessageQueuePublisher {

	private final MessageQueueProducer messageQueueProducer;
	private final MessageQueueProperties messageQueueProperties;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onGroupMemberJoined(GroupMemberJoinedEvent event) {
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			return;
		}

		byte[] payload = serializePayload(event);
		MessageQueueMessage message = new MessageQueueMessage(
			MessageQueueTopics.GROUP_MEMBER_JOINED,
			String.valueOf(event.memberId()),
			payload,
			Map.of("eventType", "GroupMemberJoinedEvent", "schemaVersion", "v1"),
			event.joinedAt(),
			null);

		messageQueueProducer.publish(message);
	}

	private byte[] serializePayload(GroupMemberJoinedEvent event) {
		GroupMemberJoinedMessagePayload payload = new GroupMemberJoinedMessagePayload(
			event.groupId(),
			event.memberId(),
			event.groupName(),
			event.joinedAt().toEpochMilli());
		try {
			return objectMapper.writeValueAsBytes(payload);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("GroupMemberJoinedEvent 메시지 직렬화에 실패했습니다", ex);
		}
	}
}
