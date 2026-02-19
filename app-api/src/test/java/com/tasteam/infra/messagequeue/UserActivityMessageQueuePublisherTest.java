package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.resilience.UserActivitySourceOutboxService;

@UnitTest
@DisplayName("사용자 이벤트 MQ publisher")
class UserActivityMessageQueuePublisherTest {

	@Test
	@DisplayName("사용자 이벤트를 발행하면 USER_ACTIVITY 토픽 메시지로 변환한다")
	void sink_publishesUserActivityMessage() throws Exception {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		UserActivitySourceOutboxService outboxService = mock(UserActivitySourceOutboxService.class);
		UserActivityMessageQueuePublisher publisher = new UserActivityMessageQueuePublisher(
			producer,
			properties,
			JsonMapper.builder().findAndAddModules().build(),
			outboxService);

		ActivityEvent event = new ActivityEvent(
			"evt-1",
			"group.joined",
			"v1",
			Instant.parse("2026-02-18T00:00:00Z"),
			101L,
			null,
			Map.of("groupId", 10L));

		// when
		publisher.sink(event);

		// then
		ArgumentCaptor<MessageQueueMessage> captor = ArgumentCaptor.forClass(MessageQueueMessage.class);
		verify(producer).publish(captor.capture());
		MessageQueueMessage message = captor.getValue();
		assertThat(message.topic()).isEqualTo(MessageQueueTopics.USER_ACTIVITY);
		assertThat(message.key()).isEqualTo("101");
		assertThat(message.messageId()).isEqualTo("evt-1");
		assertThat(message.headers())
			.containsEntry("eventType", "ActivityEvent")
			.containsEntry("eventName", "group.joined")
			.containsEntry("schemaVersion", "v1");

		ActivityEvent payload = JsonMapper.builder().findAndAddModules().build()
			.readValue(message.payload(), ActivityEvent.class);
		assertThat(payload.eventId()).isEqualTo("evt-1");
		assertThat(payload.eventName()).isEqualTo("group.joined");
		assertThat(((Number)payload.properties().get("groupId")).longValue()).isEqualTo(10L);
		verify(outboxService).markPublished("evt-1");
	}

	@Test
	@DisplayName("provider가 none이면 사용자 이벤트 발행을 건너뛴다")
	void sink_skipsWhenProviderIsNone() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.NONE.value());
		UserActivitySourceOutboxService outboxService = mock(UserActivitySourceOutboxService.class);
		UserActivityMessageQueuePublisher publisher = new UserActivityMessageQueuePublisher(
			producer,
			properties,
			JsonMapper.builder().findAndAddModules().build(),
			outboxService);
		ActivityEvent event = new ActivityEvent(
			"evt-1",
			"review.created",
			"v1",
			Instant.parse("2026-02-18T00:00:00Z"),
			null,
			"anon-1",
			Map.of("restaurantId", 55L));

		// when
		publisher.sink(event);

		// then
		verifyNoInteractions(producer);
		verifyNoInteractions(outboxService);
	}

	@Test
	@DisplayName("발행 중 예외가 발생하면 outbox 실패 상태를 기록한다")
	void sink_marksOutboxFailedWhenPublishThrows() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		org.mockito.Mockito.doThrow(new IllegalStateException("mq publish fail"))
			.when(producer)
			.publish(org.mockito.ArgumentMatchers.any(MessageQueueMessage.class));

		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		UserActivitySourceOutboxService outboxService = mock(UserActivitySourceOutboxService.class);
		UserActivityMessageQueuePublisher publisher = new UserActivityMessageQueuePublisher(
			producer,
			properties,
			JsonMapper.builder().findAndAddModules().build(),
			outboxService);

		ActivityEvent event = new ActivityEvent(
			"evt-2",
			"review.created",
			"v1",
			Instant.parse("2026-02-18T00:00:00Z"),
			10L,
			null,
			Map.of("restaurantId", 1L));

		// when & then
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> publisher.sink(event))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("mq publish fail");
		verify(outboxService).markFailed(org.mockito.ArgumentMatchers.eq("evt-2"), org.mockito.ArgumentMatchers.any());
	}
}
