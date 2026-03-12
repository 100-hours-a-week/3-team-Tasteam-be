package com.tasteam.infra.messagequeue;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultMessageBrokerSender implements MessageBrokerSender {

	private final MessageQueueProducer messageQueueProducer;

	@Override
	public void send(QueueMessage message) {
		messageQueueProducer.publish(message);
	}
}
