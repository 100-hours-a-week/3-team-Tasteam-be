package com.tasteam.infra.messagequeue;

public interface QueueEventPublisher {

	void publish(QueueTopic topic, String key, Object payload);

	void publish(QueueTopic topic, String key, Object payload, QueueEventHeaders headers);
}
