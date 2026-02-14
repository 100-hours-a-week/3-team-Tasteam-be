package com.tasteam.infra.messagequeue;

@FunctionalInterface
public interface MessageQueueMessageHandler {

	void handle(MessageQueueMessage message);
}
