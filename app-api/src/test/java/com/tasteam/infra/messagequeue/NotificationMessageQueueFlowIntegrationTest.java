package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.MessageQueueFlowTest;
import com.tasteam.domain.group.event.GroupMemberJoinedEvent;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.NotificationService;

import jakarta.annotation.Resource;

@MessageQueueFlowTest
@SpringBootTest(classes = NotificationMessageQueueFlowIntegrationTest.TestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("[통합](Notification) NotificationMessageQueueFlow 통합 테스트")
class NotificationMessageQueueFlowIntegrationTest {

	private static final String CONSUMER_GROUP = "tasteam-api";

	@Resource
	private ApplicationEventPublisher applicationEventPublisher;

	@Resource
	private MessageQueueProducer messageQueueProducer;

	@Resource
	private MessageQueueConsumer messageQueueConsumer;

	@Resource
	private NotificationService notificationService;

	@Resource
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("GroupMemberJoined 이벤트 발행 시 MQ publish와 notification 소비 처리까지 이어진다")
	void groupMemberJoinedEvent_publishAndConsume() throws Exception {
		// given
		ArgumentCaptor<QueueMessage> publishedMessageCaptor = ArgumentCaptor.forClass(QueueMessage.class);
		ArgumentCaptor<MessageQueueSubscription> subscriptionCaptor = ArgumentCaptor
			.forClass(MessageQueueSubscription.class);
		@SuppressWarnings("unchecked") ArgumentCaptor<QueueMessageHandler> handlerCaptor = ArgumentCaptor
			.forClass(
				QueueMessageHandler.class);
		verify(messageQueueConsumer).subscribe(subscriptionCaptor.capture(), handlerCaptor.capture());

		// when
		applicationEventPublisher.publishEvent(new GroupMemberJoinedEvent(10L, 20L, "스터디 그룹",
			Instant.parse("2026-02-15T00:00:00Z")));

		// then
		verify(messageQueueProducer).publish(publishedMessageCaptor.capture());
		QueueMessage publishedMessage = publishedMessageCaptor.getValue();
		assertThat(publishedMessage.topic()).isEqualTo(MessageQueueTopics.GROUP_MEMBER_JOINED);
		assertThat(publishedMessage.key()).isEqualTo("20");
		assertThat(publishedMessage.headers()).containsEntry("eventType", "GroupMemberJoinedEvent");

		GroupMemberJoinedMessagePayload payload = objectMapper.readValue(
			publishedMessage.payload(),
			GroupMemberJoinedMessagePayload.class);
		assertThat(payload.groupId()).isEqualTo(10L);
		assertThat(payload.memberId()).isEqualTo(20L);
		assertThat(payload.groupName()).isEqualTo("스터디 그룹");

		MessageQueueSubscription subscription = subscriptionCaptor.getValue();
		assertThat(subscription.topic()).isEqualTo(MessageQueueTopics.GROUP_MEMBER_JOINED);
		assertThat(subscription.consumerGroup()).isEqualTo(CONSUMER_GROUP);

		handlerCaptor.getValue().handle(QueueMessage.of(
			MessageQueueTopics.GROUP_MEMBER_JOINED,
			"20",
			objectMapper.writeValueAsBytes(payload)));

		verify(notificationService).createNotification(
			eq(20L),
			eq(NotificationType.SYSTEM),
			eq("그룹 가입 완료"),
			eq("스터디 그룹 그룹에 가입되었습니다."),
			eq("/groups/10"));
	}

	@Test
	@DisplayName("잘못된 payload를 수신하면 예외를 반환한다")
	void consumerHandler_withInvalidPayload_throwsException() {
		// given
		ArgumentCaptor<QueueMessageHandler> handlerCaptor = ArgumentCaptor
			.forClass(QueueMessageHandler.class);
		verify(messageQueueConsumer).subscribe(any(MessageQueueSubscription.class), handlerCaptor.capture());

		// when & then
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> handlerCaptor.getValue().handle(
			QueueMessage.of(
				MessageQueueTopics.GROUP_MEMBER_JOINED,
				"20",
				"{\"memberId\":\"invalid\"}".getBytes(StandardCharsets.UTF_8))))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("역직렬화");
	}

	@Configuration
	static class TestConfig {

		@Bean
		MessageQueueProperties messageQueueProperties() {
			MessageQueueProperties properties = new MessageQueueProperties();
			properties.setEnabled(true);
			properties.setProvider(MessageQueueProviderType.REDIS_STREAM.value());
			properties.setDefaultConsumerGroup("tasteam-api");
			return properties;
		}

		@Bean
		ObjectMapper objectMapper() {
			return JsonMapper.builder().findAndAddModules().build();
		}

		@Bean
		MessageQueueProducer messageQueueProducer() {
			return Mockito.mock(MessageQueueProducer.class);
		}

		@Bean
		MessageQueueConsumer messageQueueConsumer() {
			return Mockito.mock(MessageQueueConsumer.class);
		}

		@Bean
		NotificationService notificationService() {
			return Mockito.mock(NotificationService.class);
		}

		@Bean
		GroupMemberJoinedMessageQueuePublisher groupMemberJoinedMessageQueuePublisher(
			MessageQueueProducer messageQueueProducer,
			MessageQueueProperties messageQueueProperties,
			ObjectMapper objectMapper) {
			return new GroupMemberJoinedMessageQueuePublisher(messageQueueProducer, messageQueueProperties,
				objectMapper);
		}

		@Bean
		NotificationMessageQueueConsumerRegistrar notificationMessageQueueConsumerRegistrar(
			MessageQueueConsumer messageQueueConsumer,
			MessageQueueProperties messageQueueProperties,
			NotificationService notificationService,
			ObjectMapper objectMapper) {
			return new NotificationMessageQueueConsumerRegistrar(
				messageQueueConsumer,
				messageQueueProperties,
				notificationService,
				objectMapper);
		}
	}
}
