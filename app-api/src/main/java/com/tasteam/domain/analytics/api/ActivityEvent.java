package com.tasteam.domain.analytics.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 내부/외부 분석 sink로 전달되는 정규화 사용자 활동 이벤트 계약입니다.
 *
 * @param eventId 이벤트 멱등 처리를 위한 전역 식별자
 * @param eventName 이벤트 이름 (예: review.created)
 * @param eventVersion 이벤트 스키마 버전
 * @param occurredAt 이벤트 발생 시각
 * @param memberId 인증 사용자 식별자 (익명 이벤트면 null)
 * @param anonymousId 익명 사용자 식별자 (인증 이벤트면 null)
 * @param properties 이벤트 세부 속성
 */
public record ActivityEvent(
	String eventId,
	String eventName,
	String eventVersion,
	Instant occurredAt,
	Long memberId,
	String anonymousId,
	Map<String, Object> properties) {

	public ActivityEvent {
		if (isBlank(eventId)) {
			throw new IllegalArgumentException("eventId는 비어 있을 수 없습니다.");
		}
		if (isBlank(eventName)) {
			throw new IllegalArgumentException("eventName은 비어 있을 수 없습니다.");
		}
		if (isBlank(eventVersion)) {
			throw new IllegalArgumentException("eventVersion은 비어 있을 수 없습니다.");
		}
		occurredAt = Objects.requireNonNull(occurredAt, "occurredAt은 null일 수 없습니다.");
		anonymousId = normalizeNullable(anonymousId);
		properties = properties == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(properties));
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static String normalizeNullable(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}
}
