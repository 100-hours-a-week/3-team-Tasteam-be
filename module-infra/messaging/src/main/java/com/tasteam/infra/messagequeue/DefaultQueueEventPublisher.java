package com.tasteam.infra.messagequeue;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultQueueEventPublisher implements QueueEventPublisher {

	private final MessageBrokerSender messageBrokerSender;
	private final TopicNamingPolicy topicNamingPolicy;
	private final com.tasteam.infra.messagequeue.serialization.QueueMessageSerializer queueMessageSerializer;

	@Override
	public void publish(QueueTopic topic, String key, Object payload) {
		publish(topic, key, payload, QueueEventHeaders.empty());
	}

	@Override
	public void publish(QueueTopic topic, String key, Object payload, QueueEventHeaders headers) {
		String resolvedTopic = topicNamingPolicy.main(topic);
		QueueMessage message = queueMessageSerializer.createMessage(
			resolvedTopic,
			key,
			payload,
			headers.asMap());
		messageBrokerSender.send(message);
	}
}
