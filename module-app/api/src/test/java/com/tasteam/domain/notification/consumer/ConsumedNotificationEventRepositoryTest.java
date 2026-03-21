package com.tasteam.domain.notification.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import com.tasteam.config.annotation.ServiceIntegrationTest;

@ServiceIntegrationTest
@Sql(scripts = "/db/notification-outbox-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("[통합](Notification) ConsumedNotificationEventJdbcRepository 테스트")
class ConsumedNotificationEventRepositoryTest {

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	private ConsumedNotificationEventJdbcRepository repository;

	@BeforeEach
	void setUp() {
		repository = new ConsumedNotificationEventJdbcRepository(jdbcTemplate);
		jdbcTemplate.update("DELETE FROM consumed_notification_event", new MapSqlParameterSource());
	}

	// ─── tryInsert ────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("tryInsert() — Consumer-side idempotency")
	class TryInsert {

		@Test
		@DisplayName("신규 (consumerGroup, eventId) 삽입 시 true 반환")
		void tryInsert_newEvent_returnsTrue() {
			boolean result = repository.tryInsert("cg.notification.v1", "evt-new-001", "evt.notification.v1");

			assertThat(result).isTrue();
			assertThat(countByGroupAndEventId("cg.notification.v1", "evt-new-001")).isOne();
		}

		@Test
		@DisplayName("동일 (consumerGroup, eventId) 중복 삽입 시 false 반환 — row 1건만 존재")
		void tryInsert_duplicateEvent_returnsFalse() {
			repository.tryInsert("cg.notification.v1", "evt-dup-001", "evt.notification.v1");

			// 동일 (consumerGroup, eventId) 재삽입 → ON CONFLICT DO NOTHING
			boolean result = repository.tryInsert("cg.notification.v1", "evt-dup-001", "evt.notification.v1");

			assertThat(result).isFalse();
			assertThat(countByGroupAndEventId("cg.notification.v1", "evt-dup-001")).isOne();
		}

		@Test
		@DisplayName("같은 eventId라도 다른 consumerGroup이면 true 반환 — 그룹별 독립")
		void tryInsert_sameEventIdDifferentGroup_returnsTrue() {
			repository.tryInsert("cg.group-a.v1", "evt-shared-001", "evt.notification.v1");

			boolean result = repository.tryInsert("cg.group-b.v1", "evt-shared-001", "evt.notification.v1");

			assertThat(result).isTrue();
			assertThat(countByGroupAndEventId("cg.group-a.v1", "evt-shared-001")).isOne();
			assertThat(countByGroupAndEventId("cg.group-b.v1", "evt-shared-001")).isOne();
		}
	}

	// ─── deleteOlderThan ─────────────────────────────────────────────────────

	@Nested
	@DisplayName("deleteOlderThan() — 오래된 row 정리")
	class DeleteOlderThan {

		@Test
		@DisplayName("cutoff 이전 row는 삭제, 이후 row는 유지")
		void deleteOlderThan_removesStaleRows() {
			// 1시간 전에 삽입된 것처럼 직접 INSERT
			jdbcTemplate.update(
				"INSERT INTO consumed_notification_event (consumer_group, event_id, stream_key, processed_at) "
					+ "VALUES (:cg, :eid, :sk, :pa)",
				new MapSqlParameterSource()
					.addValue("cg", "cg.notification.v1")
					.addValue("eid", "evt-old-001")
					.addValue("sk", "evt.notification.v1")
					.addValue("pa", java.sql.Timestamp.from(Instant.now().minusSeconds(3600))));
			// 최근 row (1분 전)
			repository.tryInsert("cg.notification.v1", "evt-recent-001", "evt.notification.v1");

			// cutoff: 30분 전 → 1시간 전 row는 삭제, 1분 전 row는 유지
			repository.deleteOlderThan(Instant.now().minusSeconds(1800));

			assertThat(countByGroupAndEventId("cg.notification.v1", "evt-old-001")).isZero();
			assertThat(countByGroupAndEventId("cg.notification.v1", "evt-recent-001")).isOne();
		}

		@Test
		@DisplayName("모든 row가 cutoff 이후이면 아무것도 삭제하지 않는다")
		void deleteOlderThan_noRowsMatchingCutoff_nothingDeleted() {
			repository.tryInsert("cg.notification.v1", "evt-keep-001", "evt.notification.v1");
			repository.tryInsert("cg.notification.v1", "evt-keep-002", "evt.notification.v1");

			// cutoff를 과거로 설정 → 모두 유지
			repository.deleteOlderThan(Instant.now().minusSeconds(86400));

			assertThat(countByGroupAndEventId("cg.notification.v1", "evt-keep-001")).isOne();
			assertThat(countByGroupAndEventId("cg.notification.v1", "evt-keep-002")).isOne();
		}
	}

	// ─── 헬퍼 ────────────────────────────────────────────────────────────────

	private int countByGroupAndEventId(String consumerGroup, String eventId) {
		Integer count = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM consumed_notification_event "
				+ "WHERE consumer_group = :cg AND event_id = :eid",
			new MapSqlParameterSource()
				.addValue("cg", consumerGroup)
				.addValue("eid", eventId),
			Integer.class);
		return count != null ? count : 0;
	}
}
