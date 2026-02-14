package com.tasteam.infra.messagequeue;

public interface MessageQueueProducer {

	void publish(MessageQueueMessage message);

	default void publish(String topic, String key, byte[] payload) {
		publish(MessageQueueMessage.of(topic, key, payload));
	}
}
