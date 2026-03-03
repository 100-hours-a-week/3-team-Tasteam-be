package com.tasteam.domain.notification.outbox;

import java.sql.Types;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NotificationOutboxJdbcRepository {

	private static final String INSERT_SQL = """
		INSERT INTO notification_outbox (
			event_id, event_type, recipient_id, payload
		) VALUES (
			:eventId, :eventType, :recipientId, CAST(:payload AS jsonb)
		)
		ON CONFLICT (event_id) DO NOTHING
		""";

	private static final String MARK_PUBLISHED_SQL = """
		UPDATE notification_outbox
		SET status = 'PUBLISHED',
			published_at = now(),
			last_error = NULL,
			next_retry_at = NULL,
			updated_at = now()
		WHERE event_id = :eventId
		""";

	private static final String MARK_FAILED_SQL = """
		UPDATE notification_outbox
		SET retry_count = retry_count + 1,
			next_retry_at = now() + LEAST(INTERVAL '300 seconds', INTERVAL '10 seconds' * POWER(2, LEAST(retry_count, 5))),
			last_error = :errorMessage,
			status = CASE WHEN retry_count >= 5 THEN 'FAILED' ELSE 'PENDING' END,
			updated_at = now()
		WHERE event_id = :eventId
		""";

	private static final String FIND_CANDIDATES_SQL = """
		SELECT id, event_id, event_type, recipient_id, payload::text AS payload_json,
		       status, retry_count, next_retry_at
		FROM notification_outbox
		WHERE status IN ('PENDING', 'FAILED')
		  AND (next_retry_at IS NULL OR next_retry_at <= :now)
		ORDER BY id ASC
		LIMIT :limit
		""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public boolean insertIfAbsent(String eventId, String eventType, Long recipientId, String payloadJson) {
		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("eventId", eventId)
			.addValue("eventType", eventType)
			.addValue("recipientId", recipientId)
			.addValue("payload", payloadJson);
		return jdbcTemplate.update(INSERT_SQL, params) > 0;
	}

	public void markPublished(String eventId) {
		jdbcTemplate.update(MARK_PUBLISHED_SQL, new MapSqlParameterSource("eventId", eventId));
	}

	public void markFailed(String eventId, String errorMessage) {
		jdbcTemplate.update(MARK_FAILED_SQL, new MapSqlParameterSource()
			.addValue("eventId", eventId)
			.addValue("errorMessage", truncateError(errorMessage)));
	}

	public List<NotificationOutboxEntry> findCandidates(int limit) {
		int validatedLimit = Math.max(1, Math.min(limit, 500));
		return jdbcTemplate.query(
			FIND_CANDIDATES_SQL,
			new MapSqlParameterSource()
				.addValue("now", Instant.now(), Types.TIMESTAMP_WITH_TIMEZONE)
				.addValue("limit", validatedLimit),
			(rs, rowNum) -> new NotificationOutboxEntry(
				rs.getLong("id"),
				rs.getString("event_id"),
				rs.getString("event_type"),
				rs.getLong("recipient_id"),
				rs.getString("payload_json"),
				NotificationOutboxStatus.valueOf(rs.getString("status")),
				rs.getInt("retry_count"),
				rs.getTimestamp("next_retry_at") == null ? null
					: rs.getTimestamp("next_retry_at").toInstant()));
	}

	private String truncateError(String errorMessage) {
		if (errorMessage == null) {
			return null;
		}
		return errorMessage.length() <= 1000 ? errorMessage : errorMessage.substring(0, 1000);
	}
}
