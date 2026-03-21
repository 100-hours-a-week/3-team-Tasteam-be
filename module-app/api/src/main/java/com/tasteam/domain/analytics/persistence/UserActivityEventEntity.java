package com.tasteam.domain.analytics.persistence;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_activity_event")
@Getter
@NoArgsConstructor
public class UserActivityEventEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false, length = 64, unique = true)
	private String eventId;

	@Column(name = "event_name", nullable = false, length = 100)
	private String eventName;

	@Column(name = "event_version", nullable = false, length = 20)
	private String eventVersion;

	@Column(name = "occurred_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant occurredAt;

	@Column(name = "member_id")
	private Long memberId;

	@Column(name = "anonymous_id", length = 100)
	private String anonymousId;

	@Column(name = "session_id", length = 100)
	private String sessionId;

	@Column(name = "source", nullable = false, length = 20)
	private String source;

	@Column(name = "request_path", length = 255)
	private String requestPath;

	@Column(name = "request_method", length = 10)
	private String requestMethod;

	@Column(name = "device_id", length = 100)
	private String deviceId;

	@Column(name = "platform", length = 30)
	private String platform;

	@Column(name = "app_version", length = 30)
	private String appVersion;

	@Column(name = "locale", length = 20)
	private String locale;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "properties", nullable = false, columnDefinition = "jsonb")
	private Map<String, Object> properties;

	@Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
	private Instant createdAt;
}
