package com.tasteam.domain.notification.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;
import com.tasteam.infra.messagequeue.QueueEventPublisher;

@UnitTest
@DisplayName("[유닛](Notification) NotificationOutboxScanner 단위 테스트")
class NotificationOutboxScannerTest {

	@Mock
	private NotificationOutboxService outboxService;

	@Mock
	private QueueEventPublisher queueEventPublisher;

	private NotificationOutboxScanner scanner;

	@BeforeEach
	void setUp() {
		scanner = new NotificationOutboxScanner(outboxService, queueEventPublisher);
		ReflectionTestUtils.setField(scanner, "batchSize", 10);
	}

	@Nested
	@DisplayName("scan() — 후보 없을 때")
	class NoCandidates {

		@Test
		@DisplayName("후보가 없으면 MQ publish를 호출하지 않는다")
		void scan_noCandidates_skipsPublish() {
			given(outboxService.findCandidates(anyInt())).willReturn(List.of());

			scanner.scan();

			then(queueEventPublisher).shouldHaveNoInteractions();
			then(outboxService).should(never()).markPublished(anyString());
			then(outboxService).should(never()).markFailed(anyString(), anyString());
		}
	}

	@Nested
	@DisplayName("scan() — 정상 처리")
	class SuccessfulPublish {

		@Test
		@DisplayName("후보 2건 → 각각 publish 후 markPublished() 호출")
		void scan_candidates_publishesEachAndMarksPublished() {
			NotificationRequestedPayload p1 = samplePayload("evt-001");
			NotificationRequestedPayload p2 = samplePayload("evt-002");
			given(outboxService.findCandidates(10)).willReturn(List.of(p1, p2));

			scanner.scan();

			then(queueEventPublisher).should(times(2)).publish(any(), anyString(), any(), any());
			then(outboxService).should().markPublished("evt-001");
			then(outboxService).should().markPublished("evt-002");
			then(outboxService).should(never()).markFailed(anyString(), anyString());
		}

		@Test
		@DisplayName("batchSize를 findCandidates에 그대로 전달한다")
		void scan_batchSizeRespected() {
			ReflectionTestUtils.setField(scanner, "batchSize", 50);
			given(outboxService.findCandidates(50)).willReturn(List.of());

			scanner.scan();

			then(outboxService).should().findCandidates(50);
		}
	}

	@Nested
	@DisplayName("scan() — publish 실패 시 재시도 상태 유지")
	class PublishFailure {

		@Test
		@DisplayName("1번 publish 실패 → markFailed() 호출, 나머지 계속 처리")
		void scan_publishFails_marksFailedAndContinues() {
			NotificationRequestedPayload p1 = samplePayload("evt-fail-001");
			NotificationRequestedPayload p2 = samplePayload("evt-ok-001");
			given(outboxService.findCandidates(10)).willReturn(List.of(p1, p2));

			willThrow(new RuntimeException("MQ 연결 실패"))
				.given(queueEventPublisher)
				.publish(any(), eq("10"), eq(p1), any());

			scanner.scan();

			// p1은 실패 → markFailed 호출
			then(outboxService).should().markFailed(eq("evt-fail-001"), anyString());
			then(outboxService).should(never()).markPublished("evt-fail-001");

			// p2는 계속 처리됨
			then(outboxService).should().markPublished("evt-ok-001");
		}

		@Test
		@DisplayName("전체 publish 실패 시 모두 markFailed() 호출")
		void scan_allPublishFail_allMarkFailed() {
			NotificationRequestedPayload p1 = samplePayload("evt-f1");
			NotificationRequestedPayload p2 = samplePayload("evt-f2");
			given(outboxService.findCandidates(10)).willReturn(List.of(p1, p2));

			willThrow(new RuntimeException("MQ 다운")).given(queueEventPublisher)
				.publish(any(), anyString(), any(), any());

			scanner.scan();

			then(outboxService).should().markFailed(eq("evt-f1"), anyString());
			then(outboxService).should().markFailed(eq("evt-f2"), anyString());
			then(outboxService).should(never()).markPublished(anyString());
		}
	}

	private NotificationRequestedPayload samplePayload(String eventId) {
		return new NotificationRequestedPayload(
			eventId,
			"GroupMemberJoinedEvent",
			10L,
			NotificationType.SYSTEM,
			List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
			"group-joined",
			Map.of("title", "그룹 가입 완료"),
			"/groups/1",
			Instant.parse("2026-03-15T00:00:00Z"));
	}
}
