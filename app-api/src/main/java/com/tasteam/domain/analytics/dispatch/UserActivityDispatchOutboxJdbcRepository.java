package com.tasteam.domain.analytics.dispatch;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
public class UserActivityDispatchOutboxJdbcRepository {

	private static final String INSERT_PENDING_SQL = """
		INSERT INTO user_activity_dispatch_outbox (
			event_id, dispatch_target, payload, status, retry_count, next_retry_at,
			last_error, dispatched_at, created_at, updated_at
		) VALUES (
			:eventId, :dispatchTarget, CAST(:payload AS jsonb), 'PENDING', 0, NULL,
			NULL, NULL, NOW(), NOW()
		)
		ON CONFLICT (event_id, dispatch_target) DO NOTHING
		""";

	private static final String SELECT_DISPATCH_CANDIDATES_SQL = """
		SELECT id, event_id, dispatch_target, payload::text AS payload_json, status, retry_count, next_retry_at
		FROM user_activity_dispatch_outbox
		WHERE dispatch_target = :dispatchTarget
		  AND status IN ('PENDING', 'FAILED')
		  AND (next_retry_at IS NULL OR next_retry_at <= :now)
		ORDER BY id ASC
		LIMIT :limit
		""";

	private static final String MARK_DISPATCHED_SQL = """
		UPDATE user_activity_dispatch_outbox
		SET status = 'DISPATCHED',
			dispatched_at = NOW(),
			last_error = NULL,
			next_retry_at = NULL,
			updated_at = NOW()
		WHERE id = :id
		""";

	private static final String MARK_FAILED_SQL = """
		UPDATE user_activity_dispatch_outbox
		SET status = 'FAILED',
			retry_count = retry_count + 1,
			next_retry_at = NOW() + (
				LEAST(:maxBackoffSeconds, :baseBackoffSeconds * POWER(2, LEAST(retry_count, 10)))::int
				* INTERVAL '1 second'
			),
			last_error = :lastError,
			updated_at = NOW()
		WHERE id = :id
		""";

	private final NamedParameterJdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public boolean insertPendingIfAbsent(ActivityEvent event, UserActivityDispatchTarget dispatchTarget) {
		Objects.requireNonNull(event, "event는 null일 수 없습니다.");
		Objects.requireNonNull(dispatchTarget, "dispatchTarget은 null일 수 없습니다.");

		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("eventId", event.eventId())
			.addValue("dispatchTarget", dispatchTarget.name())
			.addValue("payload", serializePayload(event));
		return jdbcTemplate.update(INSERT_PENDING_SQL, params) > 0;
	}

	public List<UserActivityDispatchOutboxEntry> findDispatchCandidates(
		UserActivityDispatchTarget dispatchTarget,
		int limit,
		Instant now) {
		int validatedLimit = Math.max(1, Math.min(limit, 500));
		return jdbcTemplate.query(
			SELECT_DISPATCH_CANDIDATES_SQL,
			new MapSqlParameterSource()
				.addValue("dispatchTarget", dispatchTarget.name())
				.addValue("now", now == null ? Instant.now() : now)
				.addValue("limit", validatedLimit),
			outboxEntryRowMapper());
	}

	public void markDispatched(long id) {
		jdbcTemplate.update(MARK_DISPATCHED_SQL, new MapSqlParameterSource("id", id));
	}

	public void markFailed(long id, String lastError, Duration baseDelay, Duration maxDelay) {
		long baseBackoffSeconds = toPositiveSeconds(baseDelay, Duration.ofSeconds(10));
		long maxBackoffSeconds = toPositiveSeconds(maxDelay, Duration.ofMinutes(10));
		jdbcTemplate.update(MARK_FAILED_SQL, new MapSqlParameterSource()
			.addValue("id", id)
			.addValue("baseBackoffSeconds", baseBackoffSeconds)
			.addValue("maxBackoffSeconds", Math.max(baseBackoffSeconds, maxBackoffSeconds))
			.addValue("lastError", truncateError(lastError)));
	}

	private String serializePayload(ActivityEvent event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("사용자 이벤트 dispatch outbox payload 직렬화에 실패했습니다", ex);
		}
	}

	private RowMapper<UserActivityDispatchOutboxEntry> outboxEntryRowMapper() {
		return (rs, rowNum) -> new UserActivityDispatchOutboxEntry(
			rs.getLong("id"),
			rs.getString("event_id"),
			parseTarget(rs),
			rs.getString("payload_json"),
			parseStatus(rs),
			rs.getInt("retry_count"),
			rs.getTimestamp("next_retry_at") == null ? null : rs.getTimestamp("next_retry_at").toInstant());
	}

	private UserActivityDispatchTarget parseTarget(ResultSet rs) throws SQLException {
		return UserActivityDispatchTarget.valueOf(rs.getString("dispatch_target"));
	}

	private UserActivityDispatchOutboxStatus parseStatus(ResultSet rs) throws SQLException {
		return UserActivityDispatchOutboxStatus.valueOf(rs.getString("status"));
	}

	private String truncateError(String errorMessage) {
		if (errorMessage == null) {
			return null;
		}
		int maxLength = 1000;
		return errorMessage.length() <= maxLength ? errorMessage : errorMessage.substring(0, maxLength);
	}

	private long toPositiveSeconds(Duration duration, Duration defaultValue) {
		Duration resolved = duration == null || duration.isNegative() || duration.isZero() ? defaultValue : duration;
		return Math.max(1L, resolved.toSeconds());
	}
}
