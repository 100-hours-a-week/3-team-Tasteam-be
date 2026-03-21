package com.tasteam.domain.analytics.dispatch;

import com.tasteam.domain.analytics.api.ActivityEvent;

/**
 * dispatch outbox 이벤트를 외부 목적지로 전달하는 sink 포트입니다.
 */
public interface UserActivityDispatchSink {

	/**
	 * sink가 담당하는 dispatch 대상 유형을 반환합니다.
	 *
	 * @return dispatch 대상
	 */
	UserActivityDispatchTarget target();

	/**
	 * 이벤트를 외부 sink로 전송합니다.
	 *
	 * @param event 전송 대상 이벤트
	 */
	void dispatch(ActivityEvent event);
}
