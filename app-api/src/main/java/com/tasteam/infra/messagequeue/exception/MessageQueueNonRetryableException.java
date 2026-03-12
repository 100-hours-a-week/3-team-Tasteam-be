package com.tasteam.infra.messagequeue.exception;

public class MessageQueueNonRetryableException extends MessageQueueOperationException {

	public MessageQueueNonRetryableException(String message, String topic, String messageId, Throwable cause) {
		super(message, topic, messageId, cause);
	}

	public MessageQueueNonRetryableException(String message, String topic, String messageId) {
		super(message, topic, messageId, null);
	}
}
