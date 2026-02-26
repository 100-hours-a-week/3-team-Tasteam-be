package com.tasteam.domain.analytics.application;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tasteam.domain.analytics.api.ActivityEvent;
import com.tasteam.domain.analytics.api.ActivityEventMapper;
import com.tasteam.domain.analytics.api.ActivitySink;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 도메인 이벤트를 정규화 이벤트로 변환하고 등록된 sink로 전달하는 오케스트레이터입니다.
 * 매핑/전달 중 예외가 발생해도 도메인 흐름을 중단시키지 않도록 실패를 격리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventOrchestrator {

	private final ActivityEventMapperRegistry mapperRegistry;
	private final List<ActivitySink> activitySinks;

	/**
	 * 도메인 이벤트를 수집 경로로 전달합니다.
	 *
	 * @param domainEvent 수집 대상 도메인 이벤트
	 */
	public void handleDomainEvent(Object domainEvent) {
		if (domainEvent == null) {
			log.debug("사용자 이벤트 처리 대상이 null이어서 작업을 건너뜁니다.");
			return;
		}

		Optional<ActivityEventMapper<Object>> mapper = mapperRegistry.findMapper(domainEvent.getClass());
		if (mapper.isEmpty()) {
			log.debug("등록된 사용자 이벤트 매퍼가 없어 수집을 건너뜁니다. eventType={}", domainEvent.getClass().getName());
			return;
		}

		ActivityEvent activityEvent = mapSafely(mapper.get(), domainEvent);
		if (activityEvent == null) {
			return;
		}
		dispatchToSinks(activityEvent);
	}

	private ActivityEvent mapSafely(ActivityEventMapper<Object> mapper, Object domainEvent) {
		try {
			return mapper.map(domainEvent);
		} catch (Exception ex) {
			log.error("도메인 이벤트 매핑에 실패했습니다. eventType={}", domainEvent.getClass().getName(), ex);
			return null;
		}
	}

	private void dispatchToSinks(ActivityEvent event) {
		if (activitySinks.isEmpty()) {
			log.debug("등록된 사용자 이벤트 sink가 없어 전달을 건너뜁니다. eventName={}, eventId={}",
				event.eventName(), event.eventId());
			return;
		}

		for (ActivitySink sink : activitySinks) {
			try {
				sink.sink(event);
			} catch (Exception ex) {
				log.error("사용자 이벤트 sink 전달에 실패했습니다. sinkType={}, eventName={}, eventId={}",
					resolveSinkType(sink), event.eventName(), event.eventId(), ex);
			}
		}
	}

	private String resolveSinkType(ActivitySink sink) {
		try {
			return sink.sinkType();
		} catch (Exception ignored) {
			return sink.getClass().getSimpleName();
		}
	}
}
