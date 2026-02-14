package com.tasteam.infra.messagequeue;

public class UnsupportedMessageQueueProducer implements MessageQueueProducer {

	private final MessageQueueProviderType providerType;

	public UnsupportedMessageQueueProducer(MessageQueueProviderType providerType) {
		this.providerType = providerType;
	}

	@Override
	public void publish(MessageQueueMessage message) {
		throw new UnsupportedOperationException(
			"message queue provider 구현이 아직 없습니다: " + providerType.value());
	}
}
