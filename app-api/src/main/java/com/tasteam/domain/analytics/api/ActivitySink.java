package com.tasteam.domain.analytics.api;

import java.util.List;
import java.util.Objects;

/**
 * 정규화된 사용자 활동 이벤트를 저장소/외부 도구로 전달하는 sink 포트입니다.
 * 구현체는 내부 DB 저장, MQ 발행, 외부 분석 전송 등 목적에 맞게 확장합니다.
 */
public interface ActivitySink {

	/**
	 * sink 유형명을 반환합니다.
	 *
	 * @return 운영 로그/지표 구분에 사용할 sink 이름
	 */
	String sinkType();

	/**
	 * 단일 이벤트를 sink로 전달합니다.
	 *
	 * @param event 전달할 이벤트
	 */
	void sink(ActivityEvent event);

	/**
	 * 복수 이벤트를 sink로 전달합니다.
	 * 기본 구현은 개별 sink 호출이며, 구현체가 배치 최적화를 원하면 오버라이드합니다.
	 *
	 * @param events 전달할 이벤트 목록
	 */
	default void sinkBatch(List<ActivityEvent> events) {
		if (events == null || events.isEmpty()) {
			return;
		}
		for (ActivityEvent event : events) {
			sink(Objects.requireNonNull(event, "events의 원소는 null일 수 없습니다."));
		}
	}
}
