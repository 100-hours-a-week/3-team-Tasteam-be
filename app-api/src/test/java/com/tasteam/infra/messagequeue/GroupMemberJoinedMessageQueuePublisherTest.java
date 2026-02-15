package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.group.event.GroupMemberJoinedEvent;

@UnitTest
@DisplayName("GroupMemberJoined MQ 퍼블리셔")
class GroupMemberJoinedMessageQueuePublisherTest {

	@Test
	@DisplayName("provider가 none이면 메시지를 발행하지 않는다")
	void onGroupMemberJoined_withNoneProvider_skipsPublish() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("none");
		ObjectMapper objectMapper = new ObjectMapper();
		GroupMemberJoinedMessageQueuePublisher publisher = new GroupMemberJoinedMessageQueuePublisher(
			producer,
			properties,
			objectMapper);
		GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(10L, 20L, "테스트 그룹", Instant.now());

		// when
		publisher.onGroupMemberJoined(event);

		// then
		verifyNoInteractions(producer);
	}

	@Test
	@DisplayName("provider가 redis-stream이면 GroupMemberJoined 이벤트를 MQ로 발행한다")
	void onGroupMemberJoined_withRedisStreamProvider_publishesMessage() throws Exception {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		ObjectMapper objectMapper = new ObjectMapper();
		GroupMemberJoinedMessageQueuePublisher publisher = new GroupMemberJoinedMessageQueuePublisher(
			producer,
			properties,
			objectMapper);
		Instant joinedAt = Instant.parse("2026-02-15T00:00:00Z");
		GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(10L, 20L, "테스트 그룹", joinedAt);

		// when
		publisher.onGroupMemberJoined(event);

		// then
		ArgumentCaptor<MessageQueueMessage> messageCaptor = ArgumentCaptor.forClass(MessageQueueMessage.class);
		verify(producer).publish(messageCaptor.capture());

		MessageQueueMessage message = messageCaptor.getValue();
		assertThat(message.topic()).isEqualTo(MessageQueueTopics.GROUP_MEMBER_JOINED);
		assertThat(message.key()).isEqualTo("20");
		assertThat(message.occurredAt()).isEqualTo(joinedAt);
		assertThat(message.headers()).containsEntry("eventType", "GroupMemberJoinedEvent");

		GroupMemberJoinedMessagePayload payload = objectMapper.readValue(
			message.payload(),
			GroupMemberJoinedMessagePayload.class);
		assertThat(payload.groupId()).isEqualTo(10L);
		assertThat(payload.memberId()).isEqualTo(20L);
		assertThat(payload.groupName()).isEqualTo("테스트 그룹");
		assertThat(payload.joinedAtEpochMillis()).isEqualTo(joinedAt.toEpochMilli());
	}

	@Test
	@DisplayName("직렬화에 실패하면 예외를 반환한다")
	void onGroupMemberJoined_withSerializationFailure_throwsException() throws Exception {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		when(objectMapper.writeValueAsBytes(org.mockito.ArgumentMatchers.any()))
			.thenThrow(new JsonProcessingException("failed") {});
		GroupMemberJoinedMessageQueuePublisher publisher = new GroupMemberJoinedMessageQueuePublisher(
			producer,
			properties,
			objectMapper);
		GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(10L, 20L, "테스트 그룹", Instant.now());

		// when & then
		assertThatThrownBy(() -> publisher.onGroupMemberJoined(event))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("직렬화");
	}
}
