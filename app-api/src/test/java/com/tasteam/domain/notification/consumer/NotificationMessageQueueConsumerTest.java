package com.tasteam.domain.notification.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.notification.dispatch.NotificationDispatcher;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;
import com.tasteam.infra.messagequeue.DefaultTopicNamingPolicy;
import com.tasteam.infra.messagequeue.KafkaMessageQueueProperties;
import com.tasteam.infra.messagequeue.MessageQueueConsumer;
import com.tasteam.infra.messagequeue.MessageQueueProducer;
import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.QueueMessage;
import com.tasteam.infra.messagequeue.QueueTopic;
import com.tasteam.infra.messagequeue.TopicNamingPolicy;

@UnitTest
@DisplayName("[유닛](Notification) NotificationMessageQueueConsumer 단위 테스트")
class NotificationMessageQueueConsumerTest {

	@Test
	@DisplayName("정상 처리 시 messageProcessor.process를 호출한다")
	void handleMessage_callsProcessOnSuccess() throws Exception {
		MessageQueueConsumer messageQueueConsumer = mock(MessageQueueConsumer.class);
		NotificationMessageProcessor messageProcessor = mock(NotificationMessageProcessor.class);
		NotificationDlqPublisher dlqPublisher = mock(NotificationDlqPublisher.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		NotificationMessageQueueConsumer consumer = new NotificationMessageQueueConsumer(
			messageQueueConsumer,
			properties,
			topicNamingPolicy,
			messageProcessor,
			dlqPublisher,
			objectMapper);
		ReflectionTestUtils.setField(consumer, "maxRetries", 3);

		QueueMessage message = QueueMessage.of(
			QueueTopic.NOTIFICATION_REQUESTED.defaultMainTopic(),
			"10",
			objectMapper.valueToTree(samplePayload("evt-success")));

		ReflectionTestUtils.invokeMethod(consumer, "handleMessage", message);

		verify(messageProcessor).process(org.mockito.ArgumentMatchers.any(NotificationRequestedPayload.class));
	}

	@Test
	@DisplayName("최대 재시도 초과 시 dlqPublisher.publish를 호출한다")
	void handleMessage_callsDlqPublishWhenRetryExceeded() throws Exception {
		MessageQueueConsumer messageQueueConsumer = mock(MessageQueueConsumer.class);
		NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
		NotificationMessageProcessor messageProcessor = new NotificationMessageProcessor(dispatcher);
		NotificationDlqPublisher dlqPublisher = mock(NotificationDlqPublisher.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		NotificationMessageQueueConsumer consumer = new NotificationMessageQueueConsumer(
			messageQueueConsumer,
			properties,
			topicNamingPolicy,
			messageProcessor,
			dlqPublisher,
			objectMapper);
		ReflectionTestUtils.setField(consumer, "maxRetries", 1);

		doThrow(new IllegalStateException("fcm 실패"))
			.when(dispatcher)
			.dispatch(org.mockito.ArgumentMatchers.any(NotificationRequestedPayload.class));
		QueueMessage message = QueueMessage.of(
			QueueTopic.NOTIFICATION_REQUESTED.defaultMainTopic(),
			"10",
			objectMapper.valueToTree(samplePayload("evt-fail")));

		ReflectionTestUtils.invokeMethod(consumer, "handleMessage", message);

		verify(dlqPublisher).publish(org.mockito.ArgumentMatchers.any(QueueMessage.class));
	}

	@Test
	@DisplayName("DLQ 발행 시 원본 messageId를 유지한다")
	void dlqPublisher_preservesMessageId() throws Exception {
		MessageQueueProducer messageQueueProducer = mock(MessageQueueProducer.class);
		TopicNamingPolicy topicNamingPolicy = new DefaultTopicNamingPolicy(new KafkaMessageQueueProperties());
		NotificationDlqPublisher dlqPublisher = new NotificationDlqPublisher(messageQueueProducer, topicNamingPolicy);
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

		QueueMessage message = QueueMessage.of(
			QueueTopic.NOTIFICATION_REQUESTED.defaultMainTopic(),
			"10",
			objectMapper.valueToTree(samplePayload("evt-fail")));
		org.mockito.ArgumentCaptor<QueueMessage> captor = forClass(QueueMessage.class);

		dlqPublisher.publish(message);

		verify(messageQueueProducer).publish(captor.capture());
		assertThat(captor.getValue().messageId()).isEqualTo(message.messageId());
		assertThat(captor.getValue().topic()).isEqualTo(topicNamingPolicy.dlq(QueueTopic.NOTIFICATION_REQUESTED));
		assertThat(captor.getValue().payload()).isEqualTo(message.payload());
	}

	private NotificationRequestedPayload samplePayload(String eventId) {
		return new NotificationRequestedPayload(
			eventId,
			"notification.requested",
			10L,
			NotificationType.SYSTEM,
			List.of(NotificationChannel.PUSH),
			"group.joined",
			Map.of("groupName", "테스트 그룹"),
			"/groups/10",
			Instant.parse("2026-03-06T00:00:00Z"));
	}
}
