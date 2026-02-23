package com.tasteam.batch.ai.service;

/**
 * AI Job을 워커에게 전달하는 방식 추상화.
 * Immediate: 선점 후 인프로세스 실행. Async: 메시지 발행 후 소비 측에서 선점·실행.
 */
public interface AiJobDispatcher {

	/**
	 * Job 1건을 워커에게 전달한다. 전달 방식(동기 호출 vs 메시지 발행)은 구현체에 따름.
	 *
	 * @param jobId AiJob.id
	 */
	void dispatch(long jobId);
}
