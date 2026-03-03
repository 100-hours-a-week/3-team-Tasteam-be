package com.tasteam.infra.messagequeue;

import com.tasteam.infra.messagequeue.trace.MessageQueueTraceService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TracingMessageQueueProducer implements MessageQueueProducer {

	private final MessageQueueProducer delegate;
	private final MessageQueueProviderType providerType;
	private final MessageQueueTraceService traceService;

	@Override
	public void publish(MessageQueueMessage message) {
		delegate.publish(message);
		traceService.recordPublish(message, providerType);
	}
}
