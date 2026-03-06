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
import com.tasteam.infra.messagequeue.MessageQueueConsumer;
import com.tasteam.infra.messagequeue.MessageQueueMessage;
import com.tasteam.infra.messagequeue.MessageQueueProducer;
import com.tasteam.infra.messagequeue.MessageQueueProperties;
import com.tasteam.infra.messagequeue.MessageQueueTopics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@UnitTest
@DisplayName("[유닛](Notification) NotificationMessageQueueConsumer 단위 테스트")
class NotificationMessageQueueConsumerTest {

	@Test
	@DisplayName("정상 처리 시 process success 카운터와 latency 타이머를 기록한다")
	void handleMessage_recordsSuccessMetrics() throws Exception {
		MessageQueueConsumer messageQueueConsumer = mock(MessageQueueConsumer.class);
		MessageQueueProducer messageQueueProducer = mock(MessageQueueProducer.class);
		NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		NotificationMessageQueueConsumerMetricsCollector metricsCollector = new NotificationMessageQueueConsumerMetricsCollector(
			meterRegistry);
		NotificationMessageQueueConsumer consumer = new NotificationMessageQueueConsumer(
			messageQueueConsumer,
			properties,
			messageQueueProducer,
			dispatcher,
			objectMapper,
			metricsCollector);
		ReflectionTestUtils.setField(consumer, "maxRetries", 3);

		MessageQueueMessage message = MessageQueueMessage.of(
			MessageQueueTopics.NOTIFICATION_REQUESTED,
			"10",
			objectMapper.writeValueAsBytes(samplePayload("evt-success")));

		ReflectionTestUtils.invokeMethod(consumer, "handleMessage", message);

		assertThat(meterRegistry.get("notification.consumer.process")
			.tag("result", "success")
			.counter()
			.count()).isEqualTo(1.0);
		assertThat(meterRegistry.get("notification.consumer.process.latency")
			.tag("result", "success")
			.timer()
			.count()).isEqualTo(1L);
	}

	@Test
	@DisplayName("최대 재시도 초과 시 DLQ 발행 성공/실패 메트릭을 기록한다")
	void handleMessage_recordsDlqMetricsWhenRetryExceeded() throws Exception {
		MessageQueueConsumer messageQueueConsumer = mock(MessageQueueConsumer.class);
		MessageQueueProducer messageQueueProducer = mock(MessageQueueProducer.class);
		NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		NotificationMessageQueueConsumerMetricsCollector metricsCollector = new NotificationMessageQueueConsumerMetricsCollector(
			meterRegistry);
		NotificationMessageQueueConsumer consumer = new NotificationMessageQueueConsumer(
			messageQueueConsumer,
			properties,
			messageQueueProducer,
			dispatcher,
			objectMapper,
			metricsCollector);
		ReflectionTestUtils.setField(consumer, "maxRetries", 1);

		doThrow(new IllegalStateException("fcm 실패"))
			.when(dispatcher)
			.dispatch(org.mockito.ArgumentMatchers.any(NotificationRequestedPayload.class));
		MessageQueueMessage message = MessageQueueMessage.of(
			MessageQueueTopics.NOTIFICATION_REQUESTED,
			"10",
			objectMapper.writeValueAsBytes(samplePayload("evt-fail")));

		ReflectionTestUtils.invokeMethod(consumer, "handleMessage", message);

		verify(messageQueueProducer).publish(org.mockito.ArgumentMatchers.any(MessageQueueMessage.class));
		assertThat(meterRegistry.get("notification.consumer.process")
			.tag("result", "fail")
			.counter()
			.count()).isEqualTo(1.0);
		assertThat(meterRegistry.get("notification.consumer.dlq")
			.tag("result", "success")
			.counter()
			.count()).isEqualTo(1.0);
		assertThat(meterRegistry.get("notification.consumer.process.latency")
			.tag("result", "fail")
			.timer()
			.count()).isEqualTo(1L);
	}

	@Test
	@DisplayName("DLQ 발행 시 원본 messageId를 유지한다")
	void publishToDlq_preservesMessageId() throws Exception {
		MessageQueueConsumer messageQueueConsumer = mock(MessageQueueConsumer.class);
		MessageQueueProducer messageQueueProducer = mock(MessageQueueProducer.class);
		NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
		MessageQueueProperties properties = new MessageQueueProperties();
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		NotificationMessageQueueConsumerMetricsCollector metricsCollector = new NotificationMessageQueueConsumerMetricsCollector(
			meterRegistry);
		NotificationMessageQueueConsumer consumer = new NotificationMessageQueueConsumer(
			messageQueueConsumer,
			properties,
			messageQueueProducer,
			dispatcher,
			objectMapper,
			metricsCollector);
		ReflectionTestUtils.setField(consumer, "maxRetries", 1);

		MessageQueueMessage message = MessageQueueMessage.of(
			MessageQueueTopics.NOTIFICATION_REQUESTED,
			"10",
			objectMapper.writeValueAsBytes(samplePayload("evt-fail")));
		org.mockito.ArgumentCaptor<MessageQueueMessage> captor = forClass(MessageQueueMessage.class);

		ReflectionTestUtils.invokeMethod(consumer, "publishToDlq", message);

		verify(messageQueueProducer).publish(captor.capture());
		assertThat(captor.getValue().messageId()).isEqualTo(message.messageId());
		assertThat(captor.getValue().topic()).isEqualTo(MessageQueueTopics.NOTIFICATION_REQUESTED_DLQ);
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
