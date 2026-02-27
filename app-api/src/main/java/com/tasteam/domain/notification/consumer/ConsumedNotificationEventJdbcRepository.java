package com.tasteam.domain.notification.consumer;

import java.sql.Types;
import java.time.Instant;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ConsumedNotificationEventJdbcRepository {

	private static final String TRY_INSERT_SQL = """
		INSERT INTO consumed_notification_event (consumer_group, event_id, stream_key, processed_at)
		VALUES (:consumerGroup, :eventId, :streamKey, now())
		ON CONFLICT (consumer_group, event_id) DO NOTHING
		""";

	private static final String DELETE_OLDER_THAN_SQL = """
		DELETE FROM consumed_notification_event
		WHERE processed_at < :cutoff
		""";

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public boolean tryInsert(String consumerGroup, String eventId, String streamKey) {
		MapSqlParameterSource params = new MapSqlParameterSource()
			.addValue("consumerGroup", consumerGroup)
			.addValue("eventId", eventId)
			.addValue("streamKey", streamKey);
		return jdbcTemplate.update(TRY_INSERT_SQL, params) > 0;
	}

	public void deleteOlderThan(Instant cutoff) {
		jdbcTemplate.update(DELETE_OLDER_THAN_SQL,
			new MapSqlParameterSource().addValue("cutoff", cutoff, Types.TIMESTAMP_WITH_TIMEZONE));
	}
}
