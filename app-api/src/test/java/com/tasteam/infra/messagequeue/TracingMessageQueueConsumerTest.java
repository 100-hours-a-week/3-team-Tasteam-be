package com.tasteam.infra.messagequeue;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.trace.MessageQueueTraceService;

@UnitTest
@DisplayName("Tracing 메시지큐 consumer")
class TracingMessageQueueConsumerTest {

	@Test
	@DisplayName("소비 성공 시 trace success를 기록한다")
	void subscribe_onSuccess_recordsSuccessTrace() {
		// given
		MessageQueueConsumer delegate = mock(MessageQueueConsumer.class);
		MessageQueueTraceService traceService = mock(MessageQueueTraceService.class);
		TracingMessageQueueConsumer consumer = new TracingMessageQueueConsumer(
			delegate,
			MessageQueueProviderType.REDIS_STREAM,
			traceService);
		MessageQueueSubscription subscription = new MessageQueueSubscription("domain.group.member-joined", "group-1",
			"consumer-1");
		MessageQueueMessage message = MessageQueueMessage.of("domain.group.member-joined", "200", new byte[] {1});
		AtomicReference<MessageQueueMessageHandler> capturedHandler = new AtomicReference<>();
		doAnswer(invocation -> {
			MessageQueueMessageHandler handler = invocation.getArgument(1);
			capturedHandler.set(handler);
			return null;
		}).when(delegate).subscribe(any(MessageQueueSubscription.class), any(MessageQueueMessageHandler.class));

		// when
		consumer.subscribe(subscription, ignored -> {});
		capturedHandler.get().handle(message);

		// then
		verify(traceService).recordConsumeSuccess(any(MessageQueueMessage.class), any(MessageQueueProviderType.class),
			any(String.class), any(Long.class));
	}

	@Test
	@DisplayName("소비 실패 시 trace fail을 기록하고 예외를 재던진다")
	void subscribe_onFailure_recordsFailTraceAndRethrows() {
		// given
		MessageQueueConsumer delegate = mock(MessageQueueConsumer.class);
		MessageQueueTraceService traceService = mock(MessageQueueTraceService.class);
		TracingMessageQueueConsumer consumer = new TracingMessageQueueConsumer(
			delegate,
			MessageQueueProviderType.REDIS_STREAM,
			traceService);
		MessageQueueSubscription subscription = new MessageQueueSubscription("domain.group.member-joined", "group-1",
			"consumer-1");
		MessageQueueMessage message = MessageQueueMessage.of("domain.group.member-joined", "200", new byte[] {1});
		AtomicReference<MessageQueueMessageHandler> capturedHandler = new AtomicReference<>();
		doAnswer(invocation -> {
			MessageQueueMessageHandler handler = invocation.getArgument(1);
			capturedHandler.set(handler);
			return null;
		}).when(delegate).subscribe(any(MessageQueueSubscription.class), any(MessageQueueMessageHandler.class));

		// when
		consumer.subscribe(subscription, ignored -> {
			throw new IllegalArgumentException("실패");
		});

		// then
		assertThatThrownBy(() -> capturedHandler.get().handle(message))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("실패");
		verify(traceService).recordConsumeFail(any(MessageQueueMessage.class), any(MessageQueueProviderType.class),
			any(String.class), any(Long.class), any(Exception.class));
	}
}
