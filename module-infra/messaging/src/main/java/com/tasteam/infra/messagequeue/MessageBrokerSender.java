package com.tasteam.infra.messagequeue;

public interface MessageBrokerSender {

	void send(QueueMessage message);
}
