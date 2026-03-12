package com.tasteam.infra.messagequeue;

public interface QueueEventSubscriber {

	MessageQueueSubscription subscribe(QueueTopic topic, QueueMessageHandler handler);

	void unsubscribe(MessageQueueSubscription subscription);
}
