package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.persistence.UserActivityEventStoreService;

@UnitTest
@DisplayName("사용자 이벤트 MQ consumer registrar")
class UserActivityMessageQueueConsumerRegistrarTest {

	@Test
	@DisplayName("구독 등록 시 USER_ACTIVITY 토픽 핸들러를 등록한다")
	void subscribe_registersUserActivitySubscription() throws Exception {
		// given
		MessageQueueConsumer messageQueueConsumer = mock(MessageQueueConsumer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		properties.setDefaultConsumerGroup("tasteam-api");
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		UserActivityMessageQueueConsumerRegistrar registrar = new UserActivityMessageQueueConsumerRegistrar(
			messageQueueConsumer,
			properties,
			storeService,
			objectMapper);

		// when
		registrar.subscribe();

		// then
		ArgumentCaptor<MessageQueueSubscription> subscriptionCaptor = ArgumentCaptor
			.forClass(MessageQueueSubscription.class);
		@SuppressWarnings("unchecked") ArgumentCaptor<MessageQueueMessageHandler> handlerCaptor = ArgumentCaptor
			.forClass(
				MessageQueueMessageHandler.class);
		verify(messageQueueConsumer).subscribe(subscriptionCaptor.capture(), handlerCaptor.capture());
		assertThat(subscriptionCaptor.getValue().topic()).isEqualTo(MessageQueueTopics.USER_ACTIVITY);
		assertThat(subscriptionCaptor.getValue().consumerGroup()).isEqualTo("tasteam-api-user-activity");

		ActivityEvent event = new ActivityEvent(
			"evt-1",
			"group.joined",
			"v1",
			Instant.parse("2026-02-18T00:00:00Z"),
			20L,
			null,
			Map.of("groupId", 99L));
		handlerCaptor.getValue().handle(
			MessageQueueMessage.of(MessageQueueTopics.USER_ACTIVITY, "20", objectMapper.writeValueAsBytes(event)));
		verify(storeService).store(any(ActivityEvent.class));
	}

	@Test
	@DisplayName("provider가 none이면 구독 등록을 수행하지 않는다")
	void subscribe_skipsWhenProviderNone() {
		// given
		MessageQueueConsumer messageQueueConsumer = mock(MessageQueueConsumer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.NONE.value());
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		UserActivityMessageQueueConsumerRegistrar registrar = new UserActivityMessageQueueConsumerRegistrar(
			messageQueueConsumer,
			properties,
			storeService,
			JsonMapper.builder().findAndAddModules().build());

		// when
		registrar.subscribe();

		// then
		verifyNoInteractions(messageQueueConsumer);
	}

	@Test
	@DisplayName("잘못된 payload를 수신하면 역직렬화 예외를 반환한다")
	void subscribe_handlerThrowsWhenPayloadInvalid() {
		// given
		MessageQueueConsumer messageQueueConsumer = mock(MessageQueueConsumer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setEnabled(true);
		properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
		properties.setDefaultConsumerGroup("tasteam-api");
		UserActivityEventStoreService storeService = mock(UserActivityEventStoreService.class);
		UserActivityMessageQueueConsumerRegistrar registrar = new UserActivityMessageQueueConsumerRegistrar(
			messageQueueConsumer,
			properties,
			storeService,
			JsonMapper.builder().findAndAddModules().build());

		registrar.subscribe();

		@SuppressWarnings("unchecked") ArgumentCaptor<MessageQueueMessageHandler> handlerCaptor = ArgumentCaptor
			.forClass(
				MessageQueueMessageHandler.class);
		verify(messageQueueConsumer).subscribe(any(MessageQueueSubscription.class), handlerCaptor.capture());

		// when & then
		assertThatThrownBy(() -> handlerCaptor.getValue().handle(
			MessageQueueMessage.of(
				MessageQueueTopics.USER_ACTIVITY,
				"key",
				"{\"eventId\":\"evt\"}".getBytes(StandardCharsets.UTF_8))))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("역직렬화");
		verifyNoInteractions(storeService);
	}
}
