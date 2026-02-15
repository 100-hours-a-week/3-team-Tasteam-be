package com.tasteam.infra.messagequeue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.trace.MessageQueueTraceService;

@UnitTest
@DisplayName("Tracing 메시지큐 producer")
class TracingMessageQueueProducerTest {

	@Test
	@DisplayName("발행 성공 시 delegate와 trace를 순차 호출한다")
	void publish_callsDelegateAndTrace() {
		// given
		MessageQueueProducer delegate = mock(MessageQueueProducer.class);
		MessageQueueTraceService traceService = mock(MessageQueueTraceService.class);
		TracingMessageQueueProducer producer = new TracingMessageQueueProducer(
			delegate,
			MessageQueueProviderType.REDIS_STREAM,
			traceService);
		MessageQueueMessage message = MessageQueueMessage.of("domain.review.created", "123", new byte[] {1});

		// when
		producer.publish(message);

		// then
		verify(delegate).publish(message);
		verify(traceService).recordPublish(message, MessageQueueProviderType.REDIS_STREAM);
	}
}
