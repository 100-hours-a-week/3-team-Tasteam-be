package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.NotificationService;

@UnitTest
@DisplayName("[유닛](Notification) NotificationMessageQueueConsumerRegistrar 단위 테스트")
class NotificationMessageQueueConsumerRegistrarTest {

	@Test
	@DisplayName("provider가 none이면 구독을 등록하지 않는다")
	void subscribe_withNoneProvider_skipsSubscription() {
		// given
		QueueEventSubscriber subscriber = mock(QueueEventSubscriber.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("none");
		NotificationService notificationService = mock(NotificationService.class);
		NotificationMessageQueueConsumerRegistrar registrar = new NotificationMessageQueueConsumerRegistrar(
			subscriber,
			properties,
			notificationService,
			new ObjectMapper());

		// when
		registrar.subscribe();

		// then
		verifyNoInteractions(subscriber);
	}

	@Test
	@DisplayName("메시지를 수신하면 NotificationService로 알림 생성을 위임한다")
	void subscribe_onMessage_delegatesToNotificationService() throws Exception {
		// given
		QueueEventSubscriber subscriber = mock(QueueEventSubscriber.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		NotificationService notificationService = mock(NotificationService.class);
		ObjectMapper objectMapper = new ObjectMapper();
		AtomicReference<QueueMessageHandler> capturedHandler = new AtomicReference<>();
		MessageQueueSubscription subscription = new MessageQueueSubscription(
			QueueTopic.GROUP_MEMBER_JOINED.defaultMainTopic(),
			"cg.group.member-joined.v1",
			"group.member-joined-test");
		when(subscriber.subscribe(eq(QueueTopic.GROUP_MEMBER_JOINED), any())).thenAnswer(invocation -> {
			capturedHandler.set(invocation.getArgument(1));
			return subscription;
		});
		NotificationMessageQueueConsumerRegistrar registrar = new NotificationMessageQueueConsumerRegistrar(
			subscriber,
			properties,
			notificationService,
			objectMapper);

		// when
		registrar.subscribe();

		// then
		verify(subscriber).subscribe(eq(QueueTopic.GROUP_MEMBER_JOINED), any(QueueMessageHandler.class));
		assertThat(capturedHandler.get()).isNotNull();

		GroupMemberJoinedMessagePayload payload = new GroupMemberJoinedMessagePayload(100L, 200L, "스터디 그룹", 1L);
		capturedHandler.get().handle(QueueMessage.of(
			QueueTopic.GROUP_MEMBER_JOINED.defaultMainTopic(),
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
		QueueEventSubscriber subscriber = mock(QueueEventSubscriber.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		properties.setProvider("redis-stream");
		NotificationService notificationService = mock(NotificationService.class);
		AtomicReference<QueueMessageHandler> capturedHandler = new AtomicReference<>();
		when(subscriber.subscribe(eq(QueueTopic.GROUP_MEMBER_JOINED), any())).thenAnswer(invocation -> {
			capturedHandler.set(invocation.getArgument(1));
			return new MessageQueueSubscription(
				QueueTopic.GROUP_MEMBER_JOINED.defaultMainTopic(),
				"cg.group.member-joined.v1",
				"group.member-joined-test");
		});
		NotificationMessageQueueConsumerRegistrar registrar = new NotificationMessageQueueConsumerRegistrar(
			subscriber,
			properties,
			notificationService,
			new ObjectMapper());
		registrar.subscribe();
		assertThat(capturedHandler.get()).isNotNull();

		// when & then
		assertThatThrownBy(() -> capturedHandler.get().handle(
			QueueMessage.of(
				QueueTopic.GROUP_MEMBER_JOINED.defaultMainTopic(),
				"200",
				"{\"memberId\":\"invalid\"}".getBytes(StandardCharsets.UTF_8))))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("역직렬화");
	}
}
