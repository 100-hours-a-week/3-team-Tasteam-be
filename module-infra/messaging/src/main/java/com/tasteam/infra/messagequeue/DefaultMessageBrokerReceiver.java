package com.tasteam.infra.messagequeue;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultMessageBrokerReceiver implements MessageBrokerReceiver {

	private final MessageQueueConsumer messageQueueConsumer;

	@Override
	public void subscribe(MessageQueueSubscription subscription, QueueMessageHandler handler) {
		messageQueueConsumer.subscribe(subscription, handler);
	}

	@Override
	public void unsubscribe(MessageQueueSubscription subscription) {
		messageQueueConsumer.unsubscribe(subscription);
	}
}
