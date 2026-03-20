package com.tasteam.infra.messagequeue;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultQueueEventSubscriber implements QueueEventSubscriber {

	private final MessageBrokerReceiver messageBrokerReceiver;
	private final TopicNamingPolicy topicNamingPolicy;

	@Override
	public MessageQueueSubscription subscribe(QueueTopic topic, QueueMessageHandler handler) {
		MessageQueueSubscription subscription = new MessageQueueSubscription(
			topicNamingPolicy.main(topic),
			topicNamingPolicy.consumerGroup(topic),
			consumerName(topic));
		messageBrokerReceiver.subscribe(subscription, handler);
		return subscription;
	}

	@Override
	public void unsubscribe(MessageQueueSubscription subscription) {
		messageBrokerReceiver.unsubscribe(subscription);
	}

	private String consumerName(QueueTopic topic) {
		return topic.key() + "-" + UUID.randomUUID();
	}
}
