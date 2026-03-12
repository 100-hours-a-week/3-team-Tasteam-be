package com.tasteam.infra.messagequeue.exception;

public class MessageQueueConsumeException extends MessageQueueOperationException {

	private final String consumerGroup;

	public MessageQueueConsumeException(String topic, String messageId, String consumerGroup, Throwable cause) {
		super("메시지큐 소비에 실패했습니다. topic=%s, messageId=%s, consumerGroup=%s"
			.formatted(topic, messageId, consumerGroup), topic, messageId, cause);
		this.consumerGroup = consumerGroup;
	}

	public String getConsumerGroup() {
		return consumerGroup;
	}
}
