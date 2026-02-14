package com.tasteam.infra.messagequeue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoOpMessageQueueProducer implements MessageQueueProducer {

	@Override
	public void publish(MessageQueueMessage message) {
		log.debug("NoOpMessageQueueProducer publish skipped. topic={}, messageId={}",
			message.topic(), message.messageId());
	}
}
