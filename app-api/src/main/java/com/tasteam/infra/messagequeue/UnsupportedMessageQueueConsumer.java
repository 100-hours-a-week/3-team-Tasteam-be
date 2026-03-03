package com.tasteam.infra.messagequeue;

public class UnsupportedMessageQueueConsumer implements MessageQueueConsumer {

	private final MessageQueueProviderType providerType;

	public UnsupportedMessageQueueConsumer(MessageQueueProviderType providerType) {
		this.providerType = providerType;
	}

	@Override
	public void subscribe(MessageQueueSubscription subscription, MessageQueueMessageHandler handler) {
		throw new UnsupportedOperationException(
			"message queue provider 구현이 아직 없습니다: " + providerType.value());
	}

	@Override
	public void unsubscribe(MessageQueueSubscription subscription) {
		throw new UnsupportedOperationException(
			"message queue provider 구현이 아직 없습니다: " + providerType.value());
	}
}
