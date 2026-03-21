package com.tasteam.domain.group.event;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.infra.messagequeue.MessageQueueProducer;
import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.MessageQueueProviderType;
import com.tasteam.infra.messagequeue.QueueMessage;
import com.tasteam.infra.messagequeue.QueueTopic;
import com.tasteam.infra.messagequeue.TopicNamingPolicy;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class GroupMemberJoinedMqPublisher {

	private final MessageQueueProducer messageQueueProducer;
	private final MessageQueueProperties messageQueueProperties;
	private final TopicNamingPolicy topicNamingPolicy;
	private final ObjectMapper objectMapper;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onGroupMemberJoined(GroupMemberJoinedEvent event) {
		if (messageQueueProperties.providerType() == MessageQueueProviderType.NONE) {
			return;
		}

		JsonNode payload = serializePayload(event);
		QueueMessage message = new QueueMessage(
			topicNamingPolicy.main(QueueTopic.GROUP_MEMBER_JOINED),
			String.valueOf(event.memberId()),
			payload,
			Map.of("eventType", "GroupMemberJoinedEvent", "schemaVersion", "v1"),
			event.joinedAt(),
			null);

		messageQueueProducer.publish(message);
	}

	private JsonNode serializePayload(GroupMemberJoinedEvent event) {
		GroupMemberJoinedMessagePayload payload = new GroupMemberJoinedMessagePayload(
			event.groupId(),
			event.memberId(),
			event.groupName(),
			event.joinedAt().toEpochMilli());
		try {
			return objectMapper.valueToTree(payload);
		} catch (IllegalArgumentException ex) {
			throw new IllegalStateException("GroupMemberJoinedEvent 메시지 직렬화에 실패했습니다", ex);
		}
	}
}
