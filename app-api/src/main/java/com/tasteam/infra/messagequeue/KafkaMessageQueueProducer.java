package com.tasteam.infra.messagequeue;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KafkaMessageQueueProducer implements MessageQueueProducer {

	private final KafkaPublishSupport kafkaPublishSupport;

	@Override
	public void publish(QueueMessage message) {
		kafkaPublishSupport.publish(message);
	}
}
