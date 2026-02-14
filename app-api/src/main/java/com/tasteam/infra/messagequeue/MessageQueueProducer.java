package com.tasteam.infra.messagequeue;

/**
 * 애플리케이션에서 메시지를 발행하기 위한 추상 계약.
 * <p>
 * 구현체(예: Redis Stream, Kafka)가 바뀌어도 호출부 코드는 이 인터페이스만 의존한다.
 */
public interface MessageQueueProducer {

	/**
	 * 메시지큐에 단일 메시지를 발행한다.
	 *
	 * @param message 토픽/키/페이로드/메타데이터를 포함한 메시지
	 */
	void publish(MessageQueueMessage message);

	/**
	 * 기본 파라미터로 메시지를 생성하여 발행한다.
	 *
	 * @param topic 메시지 라우팅 토픽
	 * @param key 파티셔닝 또는 식별에 사용하는 키
	 * @param payload 직렬화된 메시지 본문
	 */
	default void publish(String topic, String key, byte[] payload) {
		publish(MessageQueueMessage.of(topic, key, payload));
	}
}
