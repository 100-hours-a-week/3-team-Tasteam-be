package com.tasteam.domain.chat.stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.chat.config.ChatStreamProperties;
import com.tasteam.domain.chat.dto.response.ChatMessageItemResponse;
import com.tasteam.domain.chat.repository.ChatRoomRepository;
import com.tasteam.domain.chat.type.ChatMessageType;

@UnitTest
@DisplayName("[유닛](Chat) ChatStreamSubscriber Pub/Sub ACK 분기 테스트")
class ChatStreamSubscriberPubSubTest {

	@Test
	@DisplayName("ws-pubsub 활성화 + Pub/Sub 발행 성공 시 ACK 한다")
	void handleRecord_pubSubSuccess_ack() {
		ChatWsBroadcastPublisher wsBroadcastPublisher = mock(ChatWsBroadcastPublisher.class);
		when(wsBroadcastPublisher.publish(any(ChatStreamPayload.class))).thenReturn(true);

		TestFixture fixture = new TestFixture(wsBroadcastPublisher, sampleProperties(true));
		MapRecord<String, String, String> record = fixture.sampleRecord();

		ReflectionTestUtils.invokeMethod(
			fixture.subscriber,
			"handleRecord",
			record,
			Integer.valueOf(3),
			"chat:partition:3");

		verify(wsBroadcastPublisher).publish(any(ChatStreamPayload.class));
		verify(fixture.streamOperations).acknowledge(anyString(), anyString(), any(RecordId.class));
		verifyNoInteractions(fixture.messagingTemplate);
	}

	@Test
	@DisplayName("ws-pubsub 활성화 + Pub/Sub 발행 실패 시 ACK 하지 않는다")
	void handleRecord_pubSubFail_noAck() {
		ChatWsBroadcastPublisher wsBroadcastPublisher = mock(ChatWsBroadcastPublisher.class);
		when(wsBroadcastPublisher.publish(any(ChatStreamPayload.class))).thenReturn(false);

		TestFixture fixture = new TestFixture(wsBroadcastPublisher, sampleProperties(true));
		MapRecord<String, String, String> record = fixture.sampleRecord();

		ReflectionTestUtils.invokeMethod(
			fixture.subscriber,
			"handleRecord",
			record,
			Integer.valueOf(3),
			"chat:partition:3");

		verify(wsBroadcastPublisher).publish(any(ChatStreamPayload.class));
		verify(fixture.streamOperations, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
		verifyNoInteractions(fixture.messagingTemplate);
	}

	private ChatStreamProperties sampleProperties(boolean wsPubSubBroadcastEnabled) {
		return new ChatStreamProperties(
			true,
			true,
			100,
			1000,
			4,
			256,
			16,
			128,
			true,
			true,
			false,
			wsPubSubBroadcastEnabled,
			"chat:websocket:broadcast");
	}

	private ChatMessageItemResponse sampleMessage() {
		return new ChatMessageItemResponse(
			101L,
			7L,
			"member",
			null,
			"hello",
			ChatMessageType.TEXT,
			List.of(),
			Instant.parse("2026-03-11T00:00:00Z"));
	}

	private class TestFixture {
		private final ChatStreamSubscriber subscriber;
		private final StreamOperations<String, Object, Object> streamOperations;
		private final SimpMessagingTemplate messagingTemplate;

		@SuppressWarnings("unchecked")
		private TestFixture(ChatWsBroadcastPublisher wsBroadcastPublisher, ChatStreamProperties properties) {
			StreamMessageListenerContainer<String, MapRecord<String, String, String>> container = mock(
				StreamMessageListenerContainer.class);
			StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
			this.streamOperations = mock(StreamOperations.class);
			lenient().when(redisTemplate.opsForStream()).thenReturn(streamOperations);
			lenient().when(streamOperations.acknowledge(anyString(), anyString(), any(RecordId.class))).thenReturn(1L);

			ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
			ChatStreamKeyResolver keyResolver = new ChatStreamKeyResolver();
			ChatStreamGroupNameProvider groupNameProvider = mock(ChatStreamGroupNameProvider.class);
			lenient().when(groupNameProvider.groupName()).thenReturn("chat-group-tasteam-api");
			lenient().when(groupNameProvider.consumerName()).thenReturn("instance-a");

			this.messagingTemplate = mock(SimpMessagingTemplate.class);
			this.subscriber = new ChatStreamSubscriber(
				container,
				redisTemplate,
				chatRoomRepository,
				keyResolver,
				groupNameProvider,
				messagingTemplate,
				properties,
				wsBroadcastPublisher,
				null);
		}

		private MapRecord<String, String, String> sampleRecord() {
			MapRecord<String, String, String> record = mock(MapRecord.class);
			when(record.getValue()).thenReturn(ChatStreamPayload.from(3L, sampleMessage()).toMap());
			lenient().when(record.getStream()).thenReturn("chat:partition:3");
			when(record.getId()).thenReturn(RecordId.of("1-0"));
			return record;
		}
	}
}
