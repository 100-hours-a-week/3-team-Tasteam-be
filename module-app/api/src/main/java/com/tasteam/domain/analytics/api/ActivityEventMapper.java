package com.tasteam.domain.analytics.api;

/**
 * 도메인 이벤트를 정규화된 {@link ActivityEvent}로 변환하는 매퍼 포트입니다.
 *
 * @param <T> 매핑 대상 도메인 이벤트 타입
 */
public interface ActivityEventMapper<T> {

	/**
	 * 매핑을 지원하는 도메인 이벤트 타입을 반환합니다.
	 *
	 * @return 지원 도메인 이벤트 클래스
	 */
	Class<T> sourceType();

	/**
	 * 도메인 이벤트를 정규화 이벤트로 변환합니다.
	 *
	 * @param event 도메인 이벤트
	 * @return 정규화 이벤트
	 */
	ActivityEvent map(T event);

	/**
	 * 입력 이벤트 타입 지원 여부를 반환합니다.
	 *
	 * @param eventType 확인할 이벤트 타입
	 * @return 지원하면 true
	 */
	default boolean supports(Class<?> eventType) {
		return sourceType().equals(eventType);
	}
}
