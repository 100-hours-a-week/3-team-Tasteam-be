package com.tasteam.domain.analytics.resilience;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasteam.domain.analytics.api.ActivityEvent;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserActivitySourceOutboxJdbcRepository {

	private static final String INSERT_PENDING_SQL = """
		INSERT INTO user_activity_source_outbox (
			event_id, event_name, event_version, occurred_at,
			member_id, payload, status, retry_count, next_retry_at,
			last_error, published_at, created_at, updated_at
		) VALUES (
			:eventId, :eventName, :eventVersion, :occurredAt,
			:memberId, CAST(:payload AS jsonb), 'PENDING', 0, NULL,
			NULL, NULL, NOW(), NOW()
		)
		ON CONFLICT (event_id) DO NOTHING
		""";

	private static final String MARK_PUBLISHED_SQL = """
		UPDATE user_activity_source_outbox
		SET status = 'PUBLISHED',
			published_at = NOW(),
			last_error = NULL,
			next_retry_at = NULL,
			updated_at = NOW()
		WHERE event_id = :eventId
		""";

	private static final String MARK_FAILED_SQL = """
		UPDATE user_activity_source_outbox
		SET status = 'FAILED',
			retry_count = retry_count + 1,
			next_retry_at = NOW() + (POWER(2, LEAST(retry_count, 6))::int * INTERVAL '10 seconds'),
			last_error = :lastError,
			updated_at = NOW()
		WHERE event_id = :eventId
		""";

	private static final String SELECT_REPLAY_CANDIDATES_SQL = """
		SELECT id, event_id, payload::text AS payload_json, status, retry_count, next_retry_at
		FROM user_activity_source_outbox
		WHERE status IN ('PENDING', 'FAILED')
		  AND (next_retry_at IS NULL OR next_retry_at <= :now)
		ORDER BY id ASC
		LIMIT :limit
		""";

	private static final String SELECT_SUMMARY_SQL = """
		SELECT
			COALESCE(SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending_count,
			COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failed_count,
			COALESCE(SUM(CASE WHEN status = 'PUBLISHED' THEN 1 ELSE 0 END), 0) AS published_count,
			COALESCE(MAX(retry_count), 0) AS max_retry_count
		FROM user_activity_source_outbox
		""";

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public boolean insertPendingIfAbsent(ActivityEvent event) {
		Objects.requireNonNull(event, "event는 null일 수 없습니다.");
		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("eventId", event.eventId())
			.addValue("eventName", event.eventName())
			.addValue("eventVersion", event.eventVersion())
			.addValue("occurredAt", event.occurredAt(), Types.TIMESTAMP_WITH_TIMEZONE)
			.addValue("memberId", event.memberId())
			.addValue("payload", serializePayload(event));
		return jdbcTemplate.update(INSERT_PENDING_SQL, params) > 0;
	}

	public void markPublished(String eventId) {
		jdbcTemplate.update(MARK_PUBLISHED_SQL, new MapSqlParameterSource("eventId", eventId));
	}

	public void markFailed(String eventId, String lastError) {
		jdbcTemplate.update(MARK_FAILED_SQL, new MapSqlParameterSource()
			.addValue("eventId", eventId)
			.addValue("lastError", truncateError(lastError)));
	}

	public List<UserActivitySourceOutboxEntry> findReplayCandidates(int limit, Instant now) {
		int validatedLimit = Math.max(1, Math.min(limit, 500));
		return jdbcTemplate.query(
			SELECT_REPLAY_CANDIDATES_SQL,
			new MapSqlParameterSource()
				.addValue("now", now == null ? Instant.now() : now, Types.TIMESTAMP_WITH_TIMEZONE)
				.addValue("limit", validatedLimit),
			outboxEntryRowMapper());
	}

	public UserActivitySourceOutboxSummary summarize() {
		UserActivitySourceOutboxSummary summary = jdbcTemplate.queryForObject(
			SELECT_SUMMARY_SQL,
			Map.of(),
			(rs, rowNum) -> new UserActivitySourceOutboxSummary(
				rs.getLong("pending_count"),
				rs.getLong("failed_count"),
				rs.getLong("published_count"),
				rs.getLong("max_retry_count")));
		if (summary == null) {
			return new UserActivitySourceOutboxSummary(0, 0, 0, 0);
		}
		return summary;
	}

	private String serializePayload(ActivityEvent event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("사용자 이벤트 outbox payload 직렬화에 실패했습니다", ex);
		}
	}

	private RowMapper<UserActivitySourceOutboxEntry> outboxEntryRowMapper() {
		return (rs, rowNum) -> new UserActivitySourceOutboxEntry(
			rs.getLong("id"),
			rs.getString("event_id"),
			rs.getString("payload_json"),
			parseStatus(rs),
			rs.getInt("retry_count"),
			rs.getTimestamp("next_retry_at") == null ? null : rs.getTimestamp("next_retry_at").toInstant());
	}

	private UserActivitySourceOutboxStatus parseStatus(ResultSet rs) throws SQLException {
		return UserActivitySourceOutboxStatus.valueOf(rs.getString("status"));
	}

	private String truncateError(String errorMessage) {
		if (errorMessage == null) {
			return null;
		}
		int maxLength = 1000;
		return errorMessage.length() <= maxLength ? errorMessage : errorMessage.substring(0, maxLength);
	}
}
