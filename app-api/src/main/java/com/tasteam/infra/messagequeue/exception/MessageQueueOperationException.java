package com.tasteam.infra.messagequeue.exception;

import lombok.Getter;

@Getter
public abstract class MessageQueueOperationException extends RuntimeException {

	private final String topic;
	private final String messageId;

	protected MessageQueueOperationException(String message, String topic, String messageId, Throwable cause) {
		super(message, cause);
		this.topic = topic;
		this.messageId = messageId;
	}
}
