package com.tasteam.domain.notification.dispatch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.notification.consumer.ConsumedNotificationEventJdbcRepository;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;
import com.tasteam.domain.notification.repository.NotificationPreferenceRepository;
import com.tasteam.domain.notification.service.FcmPushService;
import com.tasteam.domain.notification.service.NotificationService;
import com.tasteam.infra.email.EmailSender;

@UnitTest
@DisplayName("[유닛](Notification) NotificationDispatcher 단위 테스트")
class NotificationDispatcherAtomicityTest {

	@Mock
	private ConsumedNotificationEventJdbcRepository consumedEventRepository;

	@Mock
	private NotificationPreferenceRepository preferenceRepository;

	@Mock
	private NotificationService notificationService;

	@Mock
	private FcmPushService fcmPushService;

	@Mock
	private EmailSender emailSender;

	@Mock
	private MemberRepository memberRepository;

	private FcmCircuitBreaker fcmCircuitBreaker;
	private EmailCircuitBreaker emailCircuitBreaker;
	private NotificationDispatcher dispatcher;

	@BeforeEach
	void setUp() {
		fcmCircuitBreaker = new FcmCircuitBreaker(5, Duration.ofSeconds(60), java.time.Clock.systemUTC());
		emailCircuitBreaker = new EmailCircuitBreaker(3, Duration.ofSeconds(300), java.time.Clock.systemUTC());
		dispatcher = new NotificationDispatcher(
			consumedEventRepository,
			preferenceRepository,
			notificationService,
			fcmPushService,
			emailSender,
			memberRepository,
			fcmCircuitBreaker,
			emailCircuitBreaker);

		// 기본: tryInsert 성공, 선호도 없음(기본 활성화) — 일부 테스트에서 사용 안 할 수 있으므로 lenient
		lenient().when(consumedEventRepository.tryInsert(anyString(), anyString(), anyString())).thenReturn(true);
		lenient().when(preferenceRepository.findAllByMemberIdAndChannelIn(anyLong(), any())).thenReturn(List.of());
	}

	// ─── Consumer-side idempotency ────────────────────────────────────────────

	@Nested
	@DisplayName("중복 이벤트 처리 방지 (consumer-side idempotency)")
	class Idempotency {

		@Test
		@DisplayName("tryInsert가 false 반환 시 채널 발송을 전혀 호출하지 않는다")
		void dispatch_duplicateEventId_skipsProcessing() {
			given(consumedEventRepository.tryInsert(anyString(), anyString(), anyString())).willReturn(false);

			dispatcher.dispatch(webAndPushPayload("evt-dup-001"));

			then(notificationService).should(never()).createNotification(any(), anyLong(), any(), any(), any(), any());
			then(fcmPushService).should(never()).sendToMember(anyLong(), any(), any(), any());
		}
	}

	// ─── Partial channel failure isolation ───────────────────────────────────

	@Nested
	@DisplayName("채널 부분 실패 격리")
	class PartialChannelFailure {

		@Test
		@DisplayName("WEB 채널 예외 → 예외 삼킴, PUSH 채널은 계속 처리")
		void dispatch_webChannelFails_continuesOtherChannels() {
			willThrow(new RuntimeException("WEB 알림 생성 실패"))
				.given(notificationService)
				.createNotification(anyString(), anyLong(), any(), anyString(), anyString(), anyString());

			dispatcher.dispatch(webAndPushPayload("evt-web-fail-001"));

			// PUSH는 계속 호출됨
			then(fcmPushService).should().sendToMember(anyLong(), anyString(), anyString(), anyString());
		}

		@Test
		@DisplayName("PUSH 채널 예외 → 예외 삼킴, WEB 채널은 이미 처리됨")
		void dispatch_pushChannelFails_webAlreadyProcessed() {
			willThrow(new RuntimeException("FCM 실패"))
				.given(fcmPushService)
				.sendToMember(anyLong(), anyString(), anyString(), anyString());

			// WEB + PUSH 순서인 payload
			dispatcher.dispatch(webAndPushPayload("evt-push-fail-001"));

			// WEB은 이미 호출됨 (순서상 먼저 실행)
			then(notificationService).should()
				.createNotification(anyString(), anyLong(), any(), anyString(), anyString(), anyString());
		}
	}

	// ─── Circuit Breaker ─────────────────────────────────────────────────────

	@Nested
	@DisplayName("Circuit Breaker — 발송 스킵")
	class CircuitBreaker {

		@Test
		@DisplayName("FCM Circuit Breaker OPEN 시 PUSH 발송을 스킵한다")
		void dispatch_fcmCircuitOpen_skipsPushDispatch() {
			// FCM threshold(5) 초과 → OPEN 상태 강제
			for (int i = 0; i < 5; i++) {
				fcmCircuitBreaker.recordFailure();
			}

			dispatcher.dispatch(pushOnlyPayload("evt-fcm-open-001"));

			then(fcmPushService).should(never()).sendToMember(anyLong(), any(), any(), any());
		}

		@Test
		@DisplayName("Email Circuit Breaker OPEN 시 EMAIL 발송을 스킵한다")
		void dispatch_emailCircuitOpen_skipsEmailDispatch() {
			// Email threshold(3) 초과 → OPEN 상태 강제 (memberRepository는 호출되지 않음)
			for (int i = 0; i < 3; i++) {
				emailCircuitBreaker.recordFailure();
			}

			dispatcher.dispatch(emailOnlyPayload("evt-email-open-001"));

			then(emailSender).should(never()).sendTemplateEmail(anyString(), anyString(), any());
			then(memberRepository).should(never()).findById(anyLong());
		}
	}

	// ─── 헬퍼 ────────────────────────────────────────────────────────────────

	private NotificationRequestedPayload webAndPushPayload(String eventId) {
		return new NotificationRequestedPayload(
			eventId, "GroupMemberJoinedEvent", 10L,
			NotificationType.SYSTEM,
			List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
			"group-joined",
			Map.of("title", "그룹 가입", "body", "테스트 그룹에 가입되었습니다."),
			"/groups/1",
			Instant.parse("2026-03-15T00:00:00Z"));
	}

	private NotificationRequestedPayload pushOnlyPayload(String eventId) {
		return new NotificationRequestedPayload(
			eventId, "GroupMemberJoinedEvent", 10L,
			NotificationType.SYSTEM,
			List.of(NotificationChannel.PUSH),
			"group-joined",
			Map.of("title", "그룹 가입", "body", "가입 알림"),
			"/groups/1",
			Instant.parse("2026-03-15T00:00:00Z"));
	}

	private NotificationRequestedPayload emailOnlyPayload(String eventId) {
		return new NotificationRequestedPayload(
			eventId, "MemberRegisteredEvent", 10L,
			NotificationType.SYSTEM,
			List.of(NotificationChannel.EMAIL),
			"member-welcome",
			Map.of("subject", "환영합니다", "nickname", "테스터"),
			"/home",
			Instant.parse("2026-03-15T00:00:00Z"));
	}
}
