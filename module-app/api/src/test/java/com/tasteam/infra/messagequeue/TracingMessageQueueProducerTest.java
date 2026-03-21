package com.tasteam.infra.messagequeue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.config.annotation.UnitTest;
import com.tasteam.infra.messagequeue.trace.MessageQueueTraceService;

@UnitTest
@DisplayName("[유닛](Tracing) TracingMessageQueueProducer 단위 테스트")
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
		QueueMessage message = QueueMessage.of("domain.review.created", "123", new ObjectMapper().createObjectNode());

		// when
		producer.publish(message);

		// then
		verify(delegate).publish(message);
		verify(traceService).recordPublish(message, MessageQueueProviderType.REDIS_STREAM);
	}
}
