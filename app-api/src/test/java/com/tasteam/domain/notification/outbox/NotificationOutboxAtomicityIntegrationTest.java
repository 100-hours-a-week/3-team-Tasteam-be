package com.tasteam.domain.notification.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;

@ServiceIntegrationTest
@Sql(scripts = "/db/notification-outbox-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("[통합](Notification) NotificationOutboxService 원자성 통합 테스트")
class NotificationOutboxAtomicityIntegrationTest {

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private NotificationOutboxService outboxService;

	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUp() {
		jdbcTemplate.update("DELETE FROM notification_outbox", new MapSqlParameterSource());
	}

	// ─── 원자성 보장 ─────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("Outbox 원자성: BEFORE_COMMIT + MANDATORY")
	class AtomicityGuarantee {

		@Test
		@DisplayName("TX 커밋 시 outbox row가 존재한다")
		void enqueueAndCommit_outboxRowExists() {
			NotificationRequestedPayload payload = samplePayload("evt-commit-001");

			transactionTemplate.execute(status -> {
				outboxService.enqueue(payload);
				return null;
			});

			assertThat(countByEventId("evt-commit-001")).isOne();
		}

		@Test
		@DisplayName("TX 롤백 시 outbox row가 존재하지 않는다 — 핵심 원자성 보장")
		void enqueueAndRollback_outboxRowAbsent() {
			NotificationRequestedPayload payload = samplePayload("evt-rollback-001");

			assertThatThrownBy(() -> transactionTemplate.execute(status -> {
				outboxService.enqueue(payload);
				throw new RuntimeException("강제 롤백 — 도메인 실패 시뮬레이션");
			})).isInstanceOf(RuntimeException.class);

			// 도메인 TX가 rollback되면 outbox row도 함께 rollback
			assertThat(countByEventId("evt-rollback-001")).isZero();
		}

		@Test
		@DisplayName("TX 없이 enqueue() 호출 시 IllegalTransactionStateException — MANDATORY 강제")
		void enqueue_withoutActiveTransaction_throws() {
			NotificationRequestedPayload payload = samplePayload("evt-no-tx-001");

			// MANDATORY: 활성 TX가 없으면 즉시 예외 발생 → 설계 의도를 코드로 강제
			assertThatThrownBy(() -> outboxService.enqueue(payload))
				.isInstanceOf(IllegalTransactionStateException.class);

			assertThat(countByEventId("evt-no-tx-001")).isZero();
		}
	}

	// ─── 중복 처리 방지 ──────────────────────────────────────────────────────────

	@Nested
	@DisplayName("중복 eventId ON CONFLICT DO NOTHING")
	class Deduplication {

		@Test
		@DisplayName("동일 eventId로 2회 enqueue 시 row 1건만 존재한다")
		void duplicateEventId_onlyOneRowInserted() {
			NotificationRequestedPayload payload = samplePayload("evt-dup-001");

			transactionTemplate.execute(status -> {
				outboxService.enqueue(payload);
				return null;
			});
			transactionTemplate.execute(status -> {
				outboxService.enqueue(payload); // 동일 eventId → ON CONFLICT DO NOTHING
				return null;
			});

			assertThat(countByEventId("evt-dup-001")).isOne();
		}
	}

	// ─── 상태 전이 ───────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("markPublished() — 상태 PUBLISHED 전이")
	class MarkPublished {

		@Test
		@DisplayName("markPublished() 호출 후 status=PUBLISHED, published_at 설정")
		void markPublished_setsStatusPublished() {
			NotificationRequestedPayload payload = samplePayload("evt-pub-001");
			transactionTemplate.execute(status -> {
				outboxService.enqueue(payload);
				return null;
			});

			outboxService.markPublished("evt-pub-001");

			Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT status, published_at FROM notification_outbox WHERE event_id = :eventId",
				new MapSqlParameterSource("eventId", "evt-pub-001"));
			assertThat(row.get("status")).isEqualTo("PUBLISHED");
			assertThat(row.get("published_at")).isNotNull();
		}
	}

	@Nested
	@DisplayName("markFailed() — retry_count 증가 및 FAILED 전이")
	class MarkFailed {

		@Test
		@DisplayName("markFailed() 6회 호출 후 status=FAILED (retry_count >= 5 체크는 UPDATE 전 값 기준)")
		void markFailed_sixTimesReachesFailedStatus() {
			// SQL: status = CASE WHEN retry_count >= 5 THEN 'FAILED' ...
			// retry_count는 UPDATE 전 값 기준이므로 현재 retry_count=5일 때 FAILED → 6번째 호출
			NotificationRequestedPayload payload = samplePayload("evt-fail-001");
			transactionTemplate.execute(status -> {
				outboxService.enqueue(payload);
				return null;
			});

			for (int i = 0; i < 6; i++) {
				outboxService.markFailed("evt-fail-001", "publish timeout #" + i);
			}

			Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT status, retry_count FROM notification_outbox WHERE event_id = :eventId",
				new MapSqlParameterSource("eventId", "evt-fail-001"));
			assertThat(row.get("status")).isEqualTo("FAILED");
			assertThat(((Number)row.get("retry_count")).intValue()).isEqualTo(6);
		}

		@Test
		@DisplayName("markFailed() 1회 후 status는 PENDING 유지 (최대 5회 미만)")
		void markFailed_onceMaintainsPendingStatus() {
			NotificationRequestedPayload payload = samplePayload("evt-fail-once-001");
			transactionTemplate.execute(status -> {
				outboxService.enqueue(payload);
				return null;
			});

			outboxService.markFailed("evt-fail-once-001", "일시적 실패");

			Map<String, Object> row = jdbcTemplate.queryForMap(
				"SELECT status, retry_count FROM notification_outbox WHERE event_id = :eventId",
				new MapSqlParameterSource("eventId", "evt-fail-once-001"));
			assertThat(row.get("status")).isEqualTo("PENDING");
			assertThat(((Number)row.get("retry_count")).intValue()).isEqualTo(1);
		}
	}

	// ─── 후보 조회 ───────────────────────────────────────────────────────────────

	@Nested
	@DisplayName("findCandidates() — 조회 필터링")
	class FindCandidates {

		@Test
		@DisplayName("PUBLISHED 상태 row는 후보에서 제외된다")
		void findCandidates_skipsPublishedRows() {
			NotificationRequestedPayload payload = samplePayload("evt-published-skip-001");
			transactionTemplate.execute(status -> {
				outboxService.enqueue(payload);
				return null;
			});
			outboxService.markPublished("evt-published-skip-001");

			List<NotificationRequestedPayload> candidates = outboxService.findCandidates(100);

			assertThat(candidates)
				.extracting(NotificationRequestedPayload::eventId)
				.doesNotContain("evt-published-skip-001");
		}

		@Test
		@DisplayName("PENDING 상태 row는 후보에 포함된다")
		void findCandidates_includesPendingRows() {
			NotificationRequestedPayload payload = samplePayload("evt-pending-include-001");
			transactionTemplate.execute(status -> {
				outboxService.enqueue(payload);
				return null;
			});

			List<NotificationRequestedPayload> candidates = outboxService.findCandidates(100);

			assertThat(candidates)
				.extracting(NotificationRequestedPayload::eventId)
				.contains("evt-pending-include-001");
		}
	}

	// ─── 헬퍼 ────────────────────────────────────────────────────────────────────

	private int countByEventId(String eventId) {
		Integer count = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM notification_outbox WHERE event_id = :eventId",
			new MapSqlParameterSource("eventId", eventId),
			Integer.class);
		return count != null ? count : 0;
	}

	private NotificationRequestedPayload samplePayload(String eventId) {
		return new NotificationRequestedPayload(
			eventId,
			"GroupMemberJoinedEvent",
			10L,
			NotificationType.SYSTEM,
			List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
			"group-joined",
			Map.of("title", "그룹 가입 완료", "body", "테스트 그룹에 가입되었습니다."),
			"/groups/1",
			Instant.parse("2026-03-15T00:00:00Z"));
	}
}
