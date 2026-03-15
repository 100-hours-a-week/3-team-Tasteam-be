package com.tasteam.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.group.event.GroupMemberJoinedEvent;
import com.tasteam.domain.group.event.GroupRequestReviewedEvent;
import com.tasteam.domain.group.event.GroupRequestSubmittedEvent;
import com.tasteam.domain.member.event.MemberRegisteredEvent;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.outbox.NotificationOutboxService;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;

@UnitTest
@DisplayName("[유닛](Notification) NotificationDomainEventListener 단위 테스트")
class NotificationDomainEventListenerTest {

	@Mock
	private NotificationOutboxService outboxService;

	@InjectMocks
	private NotificationDomainEventListener listener;

	private static final String APP_URL = "https://tasteam.co.kr";

	private void setAppUrl() {
		ReflectionTestUtils.setField(listener, "appUrl", APP_URL);
	}

	@Nested
	@DisplayName("GroupMemberJoinedEvent")
	class OnGroupMemberJoined {

		@Test
		@DisplayName("그룹 가입 이벤트를 수신하면 WEB+PUSH 채널 payload로 아웃박스에 등록한다")
		void enqueuesPayloadWithWebAndPushChannels() {
			setAppUrl();
			GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(10L, 20L, "스터디 그룹",
				Instant.parse("2026-01-01T00:00:00Z"));
			ArgumentCaptor<NotificationRequestedPayload> captor = forClass(NotificationRequestedPayload.class);

			listener.onGroupMemberJoined(event);

			then(outboxService).should().enqueue(captor.capture());
			NotificationRequestedPayload payload = captor.getValue();
			assertThat(payload.recipientId()).isEqualTo(20L);
			assertThat(payload.notificationType()).isEqualTo(NotificationType.SYSTEM);
			assertThat(payload.channels()).containsExactlyInAnyOrder(NotificationChannel.WEB, NotificationChannel.PUSH);
			assertThat(payload.templateKey()).isEqualTo("group-joined");
			assertThat(payload.templateVariables()).containsEntry("title", "그룹 가입 완료");
			assertThat(payload.templateVariables()).containsEntry("groupId", 10L);
			assertThat(payload.deepLink()).isEqualTo("/groups/10");
			assertThat(payload.eventId()).isNotBlank();
		}

		@Test
		@DisplayName("아웃박스 등록 실패해도 예외가 전파되지 않는다")
		void doesNotPropagateOutboxFailure() {
			setAppUrl();
			GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(10L, 20L, "스터디 그룹",
				Instant.parse("2026-01-01T00:00:00Z"));
			willThrow(new RuntimeException("DB 오류")).given(outboxService).enqueue(
				org.mockito.ArgumentMatchers.any());

			assertThatCode(() -> listener.onGroupMemberJoined(event)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("GroupRequestSubmittedEvent")
	class OnGroupRequestSubmitted {

		@Test
		@DisplayName("가입 신청 이벤트를 수신하면 그룹 오너에게 WEB+PUSH 채널 payload로 아웃박스에 등록한다")
		void enqueuesPayloadToGroupOwner() {
			setAppUrl();
			GroupRequestSubmittedEvent event = new GroupRequestSubmittedEvent(
				10L, 30L, 99L, "스터디 그룹", Instant.parse("2026-01-01T00:00:00Z"));
			ArgumentCaptor<NotificationRequestedPayload> captor = forClass(NotificationRequestedPayload.class);

			listener.onGroupRequestSubmitted(event);

			then(outboxService).should().enqueue(captor.capture());
			NotificationRequestedPayload payload = captor.getValue();
			assertThat(payload.recipientId()).isEqualTo(99L);
			assertThat(payload.channels()).containsExactlyInAnyOrder(NotificationChannel.WEB, NotificationChannel.PUSH);
			assertThat(payload.templateKey()).isEqualTo("group-request-submitted");
			assertThat(payload.templateVariables()).containsEntry("title", "새 가입 신청");
			assertThat(payload.templateVariables()).containsEntry("applicantMemberId", 30L);
			assertThat(payload.deepLink()).isEqualTo("/groups/10/requests");
		}

		@Test
		@DisplayName("아웃박스 등록 실패해도 예외가 전파되지 않는다")
		void doesNotPropagateOutboxFailure() {
			setAppUrl();
			GroupRequestSubmittedEvent event = new GroupRequestSubmittedEvent(
				10L, 30L, 99L, "스터디 그룹", Instant.parse("2026-01-01T00:00:00Z"));
			willThrow(new RuntimeException("DB 오류")).given(outboxService).enqueue(
				org.mockito.ArgumentMatchers.any());

			assertThatCode(() -> listener.onGroupRequestSubmitted(event)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("GroupRequestReviewedEvent")
	class OnGroupRequestReviewed {

		@Test
		@DisplayName("가입 승인 이벤트를 수신하면 WEB+PUSH+EMAIL 채널 payload로 아웃박스에 등록한다")
		void approved_enqueuesPayloadWithAllChannels() {
			setAppUrl();
			GroupRequestReviewedEvent event = new GroupRequestReviewedEvent(
				10L, 20L, "스터디 그룹", GroupRequestReviewedEvent.ReviewResult.APPROVED, null,
				Instant.parse("2026-01-01T00:00:00Z"));
			ArgumentCaptor<NotificationRequestedPayload> captor = forClass(NotificationRequestedPayload.class);

			listener.onGroupRequestReviewed(event);

			then(outboxService).should().enqueue(captor.capture());
			NotificationRequestedPayload payload = captor.getValue();
			assertThat(payload.recipientId()).isEqualTo(20L);
			assertThat(payload.channels()).containsExactlyInAnyOrder(
				NotificationChannel.WEB, NotificationChannel.PUSH, NotificationChannel.EMAIL);
			assertThat(payload.templateKey()).isEqualTo("group-request-approved");
			assertThat(payload.templateVariables()).containsEntry("title", "가입 승인됨");
			assertThat(payload.templateVariables()).containsEntry("result", "APPROVED");
			assertThat(payload.templateVariables()).containsEntry("appUrl", APP_URL);
			assertThat(payload.deepLink()).isEqualTo("/groups/10");
		}

		@Test
		@DisplayName("가입 거절 이벤트를 수신하면 거절 템플릿으로 아웃박스에 등록한다")
		void rejected_enqueuesPayloadWithRejectedTemplate() {
			setAppUrl();
			GroupRequestReviewedEvent event = new GroupRequestReviewedEvent(
				10L, 20L, "스터디 그룹", GroupRequestReviewedEvent.ReviewResult.REJECTED, "인원 초과",
				Instant.parse("2026-01-01T00:00:00Z"));
			ArgumentCaptor<NotificationRequestedPayload> captor = forClass(NotificationRequestedPayload.class);

			listener.onGroupRequestReviewed(event);

			then(outboxService).should().enqueue(captor.capture());
			NotificationRequestedPayload payload = captor.getValue();
			assertThat(payload.templateKey()).isEqualTo("group-request-rejected");
			assertThat(payload.templateVariables()).containsEntry("title", "가입 거절됨");
			assertThat(payload.templateVariables()).containsEntry("result", "REJECTED");
			assertThat(payload.templateVariables()).containsEntry("reason", "인원 초과");
		}

		@Test
		@DisplayName("아웃박스 등록 실패해도 예외가 전파되지 않는다")
		void doesNotPropagateOutboxFailure() {
			setAppUrl();
			GroupRequestReviewedEvent event = new GroupRequestReviewedEvent(
				10L, 20L, "스터디 그룹", GroupRequestReviewedEvent.ReviewResult.APPROVED, null,
				Instant.parse("2026-01-01T00:00:00Z"));
			willThrow(new RuntimeException("DB 오류")).given(outboxService).enqueue(
				org.mockito.ArgumentMatchers.any());

			assertThatCode(() -> listener.onGroupRequestReviewed(event)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("MemberRegisteredEvent")
	class OnMemberRegistered {

		@Test
		@DisplayName("회원 가입 이벤트를 수신하면 EMAIL+PUSH 채널 payload로 아웃박스에 등록한다")
		void enqueuesPayloadWithEmailAndPushChannels() {
			setAppUrl();
			MemberRegisteredEvent event = new MemberRegisteredEvent(
				50L, "user@example.com", "홍길동", Instant.parse("2026-01-01T00:00:00Z"));
			ArgumentCaptor<NotificationRequestedPayload> captor = forClass(NotificationRequestedPayload.class);

			listener.onMemberRegistered(event);

			then(outboxService).should().enqueue(captor.capture());
			NotificationRequestedPayload payload = captor.getValue();
			assertThat(payload.recipientId()).isEqualTo(50L);
			assertThat(payload.notificationType()).isEqualTo(NotificationType.SYSTEM);
			assertThat(payload.channels()).containsExactlyInAnyOrder(
				NotificationChannel.EMAIL, NotificationChannel.PUSH);
			assertThat(payload.templateKey()).isEqualTo("member-welcome");
			assertThat(payload.templateVariables()).containsEntry("nickname", "홍길동");
			assertThat(payload.templateVariables()).containsEntry("appUrl", APP_URL);
		}

		@Test
		@DisplayName("아웃박스 등록 실패해도 예외가 전파되지 않는다")
		void doesNotPropagateOutboxFailure() {
			setAppUrl();
			MemberRegisteredEvent event = new MemberRegisteredEvent(
				50L, "user@example.com", "홍길동", Instant.parse("2026-01-01T00:00:00Z"));
			willThrow(new RuntimeException("DB 오류")).given(outboxService).enqueue(
				org.mockito.ArgumentMatchers.any());

			assertThatCode(() -> listener.onMemberRegistered(event)).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("이벤트 ID 중복 방지")
	class EventIdUniqueness {

		@Test
		@DisplayName("동일 이벤트를 두 번 수신해도 각각 고유한 eventId로 아웃박스에 등록한다")
		void eachListenerCallGeneratesUniqueEventId() {
			setAppUrl();
			GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(10L, 20L, "스터디 그룹",
				Instant.parse("2026-01-01T00:00:00Z"));
			ArgumentCaptor<NotificationRequestedPayload> captor = forClass(NotificationRequestedPayload.class);

			listener.onGroupMemberJoined(event);
			listener.onGroupMemberJoined(event);

			then(outboxService).should(org.mockito.Mockito.times(2)).enqueue(captor.capture());
			List<NotificationRequestedPayload> payloads = captor.getAllValues();
			assertThat(payloads.get(0).eventId()).isNotEqualTo(payloads.get(1).eventId());
		}
	}
}
