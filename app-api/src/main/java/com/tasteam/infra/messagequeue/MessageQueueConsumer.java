package com.tasteam.infra.messagequeue;

public interface MessageQueueConsumer {

	void subscribe(MessageQueueSubscription subscription, MessageQueueMessageHandler handler);

	void unsubscribe(MessageQueueSubscription subscription);
}
