package com.tasteam.infra.messagequeue;

public record MessageQueueSubscription(
	String topic,
	String consumerGroup,
	String consumerName) {

	public MessageQueueSubscription {
		if (topic == null || topic.isBlank()) {
			throw new IllegalArgumentException("topic은 필수입니다");
		}
		if (consumerGroup == null || consumerGroup.isBlank()) {
			throw new IllegalArgumentException("consumerGroup은 필수입니다");
		}
		if (consumerName == null || consumerName.isBlank()) {
			throw new IllegalArgumentException("consumerName은 필수입니다");
		}
	}
}
