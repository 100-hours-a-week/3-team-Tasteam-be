package com.tasteam.infra.messagequeue;

public interface MessageBrokerReceiver {

	void subscribe(MessageQueueSubscription subscription, QueueMessageHandler handler);

	void unsubscribe(MessageQueueSubscription subscription);
}
