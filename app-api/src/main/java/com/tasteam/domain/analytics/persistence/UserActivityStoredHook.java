package com.tasteam.domain.analytics.persistence;

import com.tasteam.domain.analytics.api.ActivityEvent;

/**
 * 내부 사용자 이벤트 저장 완료 후 후속 처리 확장을 위한 hook 계약입니다.
 * 구현체는 dispatch outbox 적재, 후처리 알림 등 저장 후 부가 작업을 담당합니다.
 */
public interface UserActivityStoredHook {

	/**
	 * hook 유형명을 반환합니다.
	 *
	 * @return 운영 로그/지표 식별에 사용하는 hook 이름
	 */
	String hookType();

	/**
	 * 사용자 이벤트 저장이 성공(insert)된 직후 호출됩니다.
	 * 구현체 예외는 상위 저장 흐름으로 전파되지 않도록 호출부에서 격리됩니다.
	 *
	 * @param event 저장 완료 이벤트
	 */
	void afterStored(ActivityEvent event);
}
