package com.tasteam.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.chat.dto.ChatRoomMemberSnapshot;
import com.tasteam.domain.chat.event.ChatMessageSentEvent;
import com.tasteam.domain.chat.presence.ChatRoomPresenceRegistry;
import com.tasteam.domain.chat.repository.ChatRoomMemberRepository;
import com.tasteam.domain.chat.type.ChatMessageType;
import com.tasteam.domain.notification.dto.response.AdminPushNotificationResponse;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.FcmPushService;
import com.tasteam.domain.notification.service.NotificationPreferenceService;
import com.tasteam.domain.notification.service.NotificationService;

@UnitTest
@DisplayName("[유닛](Notification) ChatNotificationEventListener 단위 테스트")
class ChatNotificationEventListenerTest {

	@Mock
	private ChatRoomMemberRepository chatRoomMemberRepository;
	@Mock
	private NotificationService notificationService;
	@Mock
	private NotificationPreferenceService preferenceService;
	@Mock
	private ChatRoomPresenceRegistry presenceRegistry;
	@Mock
	private FcmPushService fcmPushService;
	@Mock
	private ChatNotificationMetricsCollector metricsCollector;

	@InjectMocks
	private ChatNotificationEventListener listener;

	private static final Long CHAT_ROOM_ID = 1L;
	private static final Long SENDER_ID = 10L;
	private static final Long RECIPIENT_ID = 20L;
	private static final Long RECIPIENT_ID_2 = 21L;
	private static final AdminPushNotificationResponse FCM_SUCCESS = new AdminPushNotificationResponse(1, 0, 0);

	private ChatMessageSentEvent textEvent(String preview) {
		return new ChatMessageSentEvent(CHAT_ROOM_ID, 100L, SENDER_ID, "발신자", ChatMessageType.TEXT, preview,
			Instant.now());
	}

	private ChatRoomMemberSnapshot snapshot(Long memberId) {
		return new ChatRoomMemberSnapshot(memberId, null, null);
	}

	@Nested
	@DisplayName("SYSTEM 메시지 필터")
	class SystemMessageFilter {

		@Test
		@DisplayName("SYSTEM 메시지는 알림을 생성하지 않는다")
		void systemMessage_skipsAllNotifications() {
			ChatMessageSentEvent event = new ChatMessageSentEvent(
				CHAT_ROOM_ID, 100L, SENDER_ID, "시스템", ChatMessageType.SYSTEM, null, Instant.now());

			listener.onChatMessageSent(event);

			then(chatRoomMemberRepository).shouldHaveNoInteractions();
			then(notificationService).shouldHaveNoInteractions();
			then(fcmPushService).shouldHaveNoInteractions();
		}
	}

	@Nested
	@DisplayName("WEB 알림 생성")
	class WebNotification {

		@Test
		@DisplayName("수신자에게 WEB 알림을 생성한다")
		void createsWebNotificationForRecipients() {
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of(snapshot(SENDER_ID), snapshot(RECIPIENT_ID)));
			// PUSH 비활성화 → presenceRegistry 미호출
			given(preferenceService.getEnabledMap(any(), eq(NotificationChannel.PUSH), eq(NotificationType.CHAT)))
				.willReturn(Map.of(RECIPIENT_ID, false));

			listener.onChatMessageSent(textEvent("안녕하세요"));

			then(notificationService).should().createNotification(
				eq(RECIPIENT_ID), eq(NotificationType.CHAT), eq("발신자"), eq("안녕하세요"),
				eq("/chat-rooms/" + CHAT_ROOM_ID));
		}

		@Test
		@DisplayName("발신자 자신에게는 WEB 알림을 생성하지 않는다")
		void doesNotCreateNotificationForSender() {
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of(snapshot(SENDER_ID)));

			listener.onChatMessageSent(textEvent("안녕하세요"));

			then(notificationService).shouldHaveNoInteractions();
			then(fcmPushService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("채팅방에 활성 멤버가 없으면 알림을 생성하지 않는다")
		void emptyRoom_skipsAllNotifications() {
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of());

			listener.onChatMessageSent(textEvent("안녕하세요"));

			then(notificationService).shouldHaveNoInteractions();
			then(fcmPushService).shouldHaveNoInteractions();
		}

		@Test
		@DisplayName("FILE 메시지는 '파일을 보냈습니다.' 프리뷰로 알림을 생성한다")
		void fileMessage_usesFilePreview() {
			ChatMessageSentEvent event = new ChatMessageSentEvent(
				CHAT_ROOM_ID, 100L, SENDER_ID, "발신자", ChatMessageType.FILE, null, Instant.now());
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of(snapshot(SENDER_ID), snapshot(RECIPIENT_ID)));
			given(preferenceService.getEnabledMap(any(), eq(NotificationChannel.PUSH), eq(NotificationType.CHAT)))
				.willReturn(Map.of(RECIPIENT_ID, false));

			listener.onChatMessageSent(event);

			then(notificationService).should().createNotification(
				eq(RECIPIENT_ID), eq(NotificationType.CHAT), eq("발신자"), eq("파일을 보냈습니다."), anyString());
		}
	}

	@Nested
	@DisplayName("PUSH 알림 발송")
	class PushNotification {

		@Test
		@DisplayName("PUSH 선호도가 활성화된 오프라인 수신자에게 FCM을 발송한다")
		void offlineRecipient_withPushEnabled_sendsFcm() {
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of(snapshot(SENDER_ID), snapshot(RECIPIENT_ID)));
			given(preferenceService.getEnabledMap(any(), eq(NotificationChannel.PUSH), eq(NotificationType.CHAT)))
				.willReturn(Map.of(RECIPIENT_ID, true));
			given(presenceRegistry.isMemberOnline(CHAT_ROOM_ID, RECIPIENT_ID)).willReturn(false);
			given(fcmPushService.sendToMembers(anySet(), anyString(), anyString(), anyString()))
				.willReturn(FCM_SUCCESS);

			listener.onChatMessageSent(textEvent("안녕하세요"));

			then(fcmPushService).should().sendToMembers(
				eq(Set.of(RECIPIENT_ID)), eq("발신자"), eq("안녕하세요"), eq("/chat-rooms/" + CHAT_ROOM_ID));
		}

		@Test
		@DisplayName("온라인 수신자에게는 FCM을 발송하지 않는다")
		void onlineRecipient_skipsFcm() {
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of(snapshot(SENDER_ID), snapshot(RECIPIENT_ID)));
			given(preferenceService.getEnabledMap(any(), eq(NotificationChannel.PUSH), eq(NotificationType.CHAT)))
				.willReturn(Map.of(RECIPIENT_ID, true));
			given(presenceRegistry.isMemberOnline(CHAT_ROOM_ID, RECIPIENT_ID)).willReturn(true);

			listener.onChatMessageSent(textEvent("안녕하세요"));

			then(fcmPushService).should(never()).sendToMembers(anySet(), anyString(), anyString(), anyString());
		}

		@Test
		@DisplayName("PUSH 선호도가 비활성화된 수신자에게는 FCM을 발송하지 않는다")
		void pushDisabledRecipient_skipsFcm() {
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of(snapshot(SENDER_ID), snapshot(RECIPIENT_ID)));
			// false → pushEligible 비어 있음 → presenceRegistry 미호출
			given(preferenceService.getEnabledMap(any(), eq(NotificationChannel.PUSH), eq(NotificationType.CHAT)))
				.willReturn(Map.of(RECIPIENT_ID, false));

			listener.onChatMessageSent(textEvent("안녕하세요"));

			then(fcmPushService).should(never()).sendToMembers(anySet(), anyString(), anyString(), anyString());
		}

		@Test
		@DisplayName("여러 수신자 중 오프라인 + PUSH 활성화된 대상만 FCM을 발송한다")
		void multipleRecipients_onlySendsToEligible() {
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of(snapshot(SENDER_ID), snapshot(RECIPIENT_ID), snapshot(RECIPIENT_ID_2)));
			// RECIPIENT_ID_2는 false → pushEligible에 포함되지 않아 presenceRegistry 미호출
			given(preferenceService.getEnabledMap(any(), eq(NotificationChannel.PUSH), eq(NotificationType.CHAT)))
				.willReturn(Map.of(RECIPIENT_ID, true, RECIPIENT_ID_2, false));
			given(presenceRegistry.isMemberOnline(CHAT_ROOM_ID, RECIPIENT_ID)).willReturn(false);
			given(fcmPushService.sendToMembers(anySet(), anyString(), anyString(), anyString()))
				.willReturn(FCM_SUCCESS);

			listener.onChatMessageSent(textEvent("안녕하세요"));

			then(fcmPushService).should().sendToMembers(
				eq(Set.of(RECIPIENT_ID)), anyString(), anyString(), anyString());
		}

		@Test
		@DisplayName("FCM 발송 실패해도 예외가 전파되지 않는다")
		void fcmFailure_doesNotPropagate() {
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of(snapshot(SENDER_ID), snapshot(RECIPIENT_ID)));
			given(preferenceService.getEnabledMap(any(), eq(NotificationChannel.PUSH), eq(NotificationType.CHAT)))
				.willReturn(Map.of(RECIPIENT_ID, true));
			given(presenceRegistry.isMemberOnline(CHAT_ROOM_ID, RECIPIENT_ID)).willReturn(false);
			willThrow(new RuntimeException("FCM 오류")).given(fcmPushService)
				.sendToMembers(anySet(), anyString(), anyString(), anyString());

			assertThatCode(() -> listener.onChatMessageSent(textEvent("안녕하세요")))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("메시지 프리뷰 처리")
	class MessagePreview {

		@Test
		@DisplayName("50 코드포인트를 초과하는 메시지는 잘라서 body로 사용한다")
		void longMessage_truncatesPreviewTo50CodePoints() {
			String longMsg = "가".repeat(60);
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of(snapshot(SENDER_ID), snapshot(RECIPIENT_ID)));
			given(preferenceService.getEnabledMap(any(), any(), any()))
				.willReturn(Map.of(RECIPIENT_ID, false));

			listener.onChatMessageSent(textEvent(longMsg));

			then(notificationService).should().createNotification(
				eq(RECIPIENT_ID), eq(NotificationType.CHAT), anyString(),
				eq("가".repeat(50)), anyString());
		}

		@Test
		@DisplayName("발신자 닉네임이 null이면 '새 메시지'를 알림 제목으로 사용한다")
		void nullSenderNickname_usesDefaultTitle() {
			ChatMessageSentEvent event = new ChatMessageSentEvent(
				CHAT_ROOM_ID, 100L, SENDER_ID, null, ChatMessageType.TEXT, "내용", Instant.now());
			given(chatRoomMemberRepository.findActiveMemberSnapshots(CHAT_ROOM_ID))
				.willReturn(List.of(snapshot(SENDER_ID), snapshot(RECIPIENT_ID)));
			given(preferenceService.getEnabledMap(any(), any(), any()))
				.willReturn(Map.of(RECIPIENT_ID, false));

			listener.onChatMessageSent(event);

			then(notificationService).should().createNotification(
				eq(RECIPIENT_ID), eq(NotificationType.CHAT), eq("새 메시지"), anyString(), anyString());
		}
	}
}
