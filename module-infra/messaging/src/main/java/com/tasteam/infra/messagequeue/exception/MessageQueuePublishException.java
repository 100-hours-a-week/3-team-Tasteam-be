package com.tasteam.infra.messagequeue.exception;

public class MessageQueuePublishException extends MessageQueueOperationException {

	public MessageQueuePublishException(String topic, String messageId, Throwable cause) {
		super("메시지큐 발행에 실패했습니다. topic=%s, messageId=%s".formatted(topic, messageId),
			topic, messageId, cause);
	}
}
