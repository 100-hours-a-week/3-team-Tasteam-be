package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.NotificationService;

@UnitTest
@DisplayName("Notification MQ 컨슈머 등록기")
class NotificationMessageQueueConsumerRegistrarTest {

	@Test
	@DisplayName("provider가 none이면 구독을 등록하지 않는다")
	void subscribe_withNoneProvider_skipsSubscription() {
		// given
		MessageQueueConsumer consumer = mock(MessageQueueConsumer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("none");
		NotificationService notificationService = mock(NotificationService.class);
		NotificationMessageQueueConsumerRegistrar registrar = new NotificationMessageQueueConsumerRegistrar(
			consumer,
			properties,
			notificationService,
			new ObjectMapper());

		// when
		registrar.subscribe();

		// then
		verifyNoInteractions(consumer);
	}

	@Test
	@DisplayName("메시지를 수신하면 NotificationService로 알림 생성을 위임한다")
	void subscribe_onMessage_delegatesToNotificationService() throws Exception {
		// given
		MessageQueueConsumer consumer = mock(MessageQueueConsumer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		properties.setDefaultConsumerGroup("tasteam-api");
		NotificationService notificationService = mock(NotificationService.class);
		ObjectMapper objectMapper = new ObjectMapper();
		NotificationMessageQueueConsumerRegistrar registrar = new NotificationMessageQueueConsumerRegistrar(
			consumer,
			properties,
			notificationService,
			objectMapper);

		// when
		registrar.subscribe();

		// then
		ArgumentCaptor<MessageQueueSubscription> subscriptionCaptor = ArgumentCaptor
			.forClass(MessageQueueSubscription.class);
		@SuppressWarnings("unchecked") ArgumentCaptor<MessageQueueMessageHandler> handlerCaptor = ArgumentCaptor
			.forClass(
				MessageQueueMessageHandler.class);
		verify(consumer).subscribe(subscriptionCaptor.capture(), handlerCaptor.capture());
		assertThat(subscriptionCaptor.getValue().topic()).isEqualTo(MessageQueueTopics.GROUP_MEMBER_JOINED);
		assertThat(subscriptionCaptor.getValue().consumerGroup()).isEqualTo("tasteam-api");

		GroupMemberJoinedMessagePayload payload = new GroupMemberJoinedMessagePayload(100L, 200L, "스터디 그룹", 1L);
		handlerCaptor.getValue().handle(MessageQueueMessage.of(
			MessageQueueTopics.GROUP_MEMBER_JOINED,
			"200",
			objectMapper.writeValueAsBytes(payload)));

		verify(notificationService).createNotification(
			eq(200L),
			eq(NotificationType.SYSTEM),
			eq("그룹 가입 완료"),
			eq("스터디 그룹 그룹에 가입되었습니다."),
			eq("/groups/100"));
	}

	@Test
	@DisplayName("잘못된 payload를 수신하면 역직렬화 예외를 반환한다")
	void subscribe_onInvalidPayload_throwsException() {
		// given
		MessageQueueConsumer consumer = mock(MessageQueueConsumer.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		NotificationService notificationService = mock(NotificationService.class);
		NotificationMessageQueueConsumerRegistrar registrar = new NotificationMessageQueueConsumerRegistrar(
			consumer,
			properties,
			notificationService,
			new ObjectMapper());
		registrar.subscribe();
		ArgumentCaptor<MessageQueueMessageHandler> handlerCaptor = ArgumentCaptor
			.forClass(MessageQueueMessageHandler.class);
		verify(consumer).subscribe(any(MessageQueueSubscription.class), handlerCaptor.capture());

		// when & then
		assertThatThrownBy(() -> handlerCaptor.getValue().handle(
			MessageQueueMessage.of(
				MessageQueueTopics.GROUP_MEMBER_JOINED,
				"200",
				"{\"memberId\":\"invalid\"}".getBytes(StandardCharsets.UTF_8))))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("역직렬화");
	}
}
