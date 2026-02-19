package com.tasteam.domain.analytics.persistence;

import java.util.Map;
import java.util.Objects;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.analytics.api.ActivityEvent;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserActivityEventJdbcRepository {

	private static final String DEFAULT_SOURCE = "SERVER";

	private static final String INSERT_SQL = """
		INSERT INTO user_activity_event (
			event_id, event_name, event_version, occurred_at,
			member_id, anonymous_id, session_id, source,
			request_path, request_method, device_id, platform,
			app_version, locale, properties, created_at
		) VALUES (
			:eventId, :eventName, :eventVersion, :occurredAt,
			:memberId, :anonymousId, :sessionId, :source,
			:requestPath, :requestMethod, :deviceId, :platform,
			:appVersion, :locale, CAST(:properties AS jsonb), NOW()
		)
		ON CONFLICT (event_id) DO NOTHING
		""";

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public boolean insertIgnoreDuplicate(ActivityEvent event) {
		Objects.requireNonNull(event, "event는 null일 수 없습니다.");
		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("eventId", event.eventId())
			.addValue("eventName", event.eventName())
			.addValue("eventVersion", event.eventVersion())
			.addValue("occurredAt", event.occurredAt())
			.addValue("memberId", event.memberId())
			.addValue("anonymousId", event.anonymousId())
			.addValue("sessionId", null)
			.addValue("source", DEFAULT_SOURCE)
			.addValue("requestPath", null)
			.addValue("requestMethod", null)
			.addValue("deviceId", null)
			.addValue("platform", null)
			.addValue("appVersion", null)
			.addValue("locale", null)
			.addValue("properties", serializeProperties(event.properties()));
		return jdbcTemplate.update(INSERT_SQL, params) > 0;
	}

	private String serializeProperties(Map<String, Object> properties) {
		try {
			return objectMapper.writeValueAsString(properties == null ? Map.of() : properties);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("사용자 이벤트 properties 직렬화에 실패했습니다", ex);
		}
	}
}
