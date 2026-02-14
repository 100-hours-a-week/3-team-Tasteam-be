package com.tasteam.infra.messagequeue;

/**
 * 메시지큐 구독과 소비를 위한 추상 계약.
 * <p>
 * 애플리케이션은 구독 규칙과 핸들러만 전달하고, 실제 소비 방식은 구현체가 담당한다.
 */
public interface MessageQueueConsumer {

	/**
	 * 구독 정보를 기준으로 메시지 소비를 시작한다.
	 *
	 * @param subscription 토픽/컨슈머 그룹/컨슈머 식별 정보를 담은 구독 설정
	 * @param handler 메시지 수신 시 호출되는 처리기
	 */
	void subscribe(MessageQueueSubscription subscription, MessageQueueMessageHandler handler);

	/**
	 * 지정한 구독의 메시지 소비를 중단한다.
	 *
	 * @param subscription 중단할 구독 설정
	 */
	void unsubscribe(MessageQueueSubscription subscription);
}
