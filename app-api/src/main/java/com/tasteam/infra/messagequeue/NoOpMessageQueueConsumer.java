package com.tasteam.infra.messagequeue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoOpMessageQueueConsumer implements MessageQueueConsumer {

	@Override
	public void subscribe(MessageQueueSubscription subscription, MessageQueueMessageHandler handler) {
		log.debug("NoOpMessageQueueConsumer subscribe skipped. topic={}, consumerGroup={}",
			subscription.topic(), subscription.consumerGroup());
	}

	@Override
	public void unsubscribe(MessageQueueSubscription subscription) {
		log.debug("NoOpMessageQueueConsumer unsubscribe skipped. topic={}, consumerGroup={}",
			subscription.topic(), subscription.consumerGroup());
	}
}
