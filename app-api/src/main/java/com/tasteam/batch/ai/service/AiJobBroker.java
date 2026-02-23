package com.tasteam.batch.ai.service;

/**
 * AI Job을 실행 측에 전달하는 방식 추상화.
 */
public interface AiJobBroker {

	/**
	 * Job 1건을 발행한다. 전달 방식(동기 호출 vs 메시지 발행)은 구현체에 따름.
	 *
	 * @param jobId AiJob.id
	 */
	void publish(long jobId);
}
