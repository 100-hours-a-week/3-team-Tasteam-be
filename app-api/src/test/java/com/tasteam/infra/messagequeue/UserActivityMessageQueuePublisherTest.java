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
		UserActivityMessageQueuePublisher publisher = new UserActivityMessageQueuePublisher(
			producer,
			properties,
			JsonMapper.builder().findAndAddModules().build());

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
	}

	@Test
	@DisplayName("provider가 none이면 사용자 이벤트 발행을 건너뛴다")
	void sink_skipsWhenProviderIsNone() {
		// given
		MessageQueueProducer producer = mock(MessageQueueProducer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.NONE.value());
		UserActivityMessageQueuePublisher publisher = new UserActivityMessageQueuePublisher(
			producer,
			properties,
			JsonMapper.builder().findAndAddModules().build());
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
	}
}
