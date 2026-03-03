package com.tasteam.infra.messagequeue;

import com.tasteam.infra.messagequeue.trace.MessageQueueTraceService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TracingMessageQueueConsumer implements MessageQueueConsumer {

	private final MessageQueueConsumer delegate;
	private final MessageQueueProviderType providerType;
	private final MessageQueueTraceService traceService;

	@Override
	public void subscribe(MessageQueueSubscription subscription, MessageQueueMessageHandler handler) {
		delegate.subscribe(subscription, message -> {
			long startedAtNanos = System.nanoTime();
			try {
				handler.handle(message);
				traceService.recordConsumeSuccess(
					message,
					providerType,
					subscription.consumerGroup(),
					toMillis(startedAtNanos));
			} catch (Exception ex) {
				traceService.recordConsumeFail(
					message,
					providerType,
					subscription.consumerGroup(),
					toMillis(startedAtNanos),
					ex);
				throw ex;
			}
		});
	}

	@Override
	public void unsubscribe(MessageQueueSubscription subscription) {
		delegate.unsubscribe(subscription);
	}

	private long toMillis(long startedAtNanos) {
		return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
	}
}
