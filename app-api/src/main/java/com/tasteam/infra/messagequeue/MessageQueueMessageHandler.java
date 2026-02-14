package com.tasteam.infra.messagequeue;

/**
 * 메시지 소비 시 비즈니스 로직을 위임받아 처리하는 함수형 계약.
 */
@FunctionalInterface
public interface MessageQueueMessageHandler {

	/**
	 * 단일 메시지를 처리한다.
	 *
	 * @param message 큐에서 전달된 메시지
	 */
	void handle(MessageQueueMessage message);
}
