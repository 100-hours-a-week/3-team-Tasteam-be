package com.tasteam.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.NotificationBroadcastService;

@UnitTest
@DisplayName("[유닛](Notification) AdminBroadcastNotificationEventListener 단위 테스트")
class AdminBroadcastNotificationEventListenerTest {

	@Mock
	private NotificationBroadcastService notificationBroadcastService;

	@InjectMocks
	private AdminBroadcastNotificationEventListener listener;

	private AdminBroadcastRequestedEvent event(Set<NotificationChannel> channels, String templateKey) {
		return new AdminBroadcastRequestedEvent(
			NotificationType.SYSTEM, "공지", "내용입니다.", "/notice", channels, templateKey, Map.of());
	}

	@Nested
	@DisplayName("WEB 채널")
	class WebChannel {

		@Test
		@DisplayName("WEB 채널 이벤트를 수신하면 broadcastWeb을 호출한다")
		void webChannel_callsBroadcastWeb() {
			listener.onAdminBroadcastRequested(event(Set.of(NotificationChannel.WEB), null));

			then(notificationBroadcastService).should().broadcastWeb(
				NotificationType.SYSTEM, "공지", "내용입니다.", "/notice");
			then(notificationBroadcastService).should(never()).broadcastPush(any(), any(), any(), any());
			then(notificationBroadcastService).should(never()).broadcastEmail(any(), any(), any());
		}
	}

	@Nested
	@DisplayName("PUSH 채널")
	class PushChannel {

		@Test
		@DisplayName("PUSH 채널 이벤트를 수신하면 broadcastPush를 호출한다")
		void pushChannel_callsBroadcastPush() {
			listener.onAdminBroadcastRequested(event(Set.of(NotificationChannel.PUSH), null));

			then(notificationBroadcastService).should().broadcastPush(
				NotificationType.SYSTEM, "공지", "내용입니다.", "/notice");
			then(notificationBroadcastService).should(never()).broadcastWeb(any(), any(), any(), any());
			then(notificationBroadcastService).should(never()).broadcastEmail(any(), any(), any());
		}
	}

	@Nested
	@DisplayName("EMAIL 채널")
	class EmailChannel {

		@Test
		@DisplayName("templateKey가 있으면 broadcastEmail을 호출한다")
		void withTemplateKey_callsBroadcastEmail() {
			listener.onAdminBroadcastRequested(event(Set.of(NotificationChannel.EMAIL), "notice-template"));

			then(notificationBroadcastService).should().broadcastEmail(
				NotificationType.SYSTEM, "notice-template", Map.of());
		}

		@Test
		@DisplayName("templateKey가 null이면 broadcastEmail을 호출하지 않는다")
		void nullTemplateKey_skipsBroadcastEmail() {
			listener.onAdminBroadcastRequested(event(Set.of(NotificationChannel.EMAIL), null));

			then(notificationBroadcastService).should(never()).broadcastEmail(any(), any(), any());
		}

		@Test
		@DisplayName("templateKey가 빈 문자열이면 broadcastEmail을 호출하지 않는다")
		void blankTemplateKey_skipsBroadcastEmail() {
			listener.onAdminBroadcastRequested(event(Set.of(NotificationChannel.EMAIL), "  "));

			then(notificationBroadcastService).should(never()).broadcastEmail(any(), any(), any());
		}
	}

	@Nested
	@DisplayName("다중 채널")
	class MultiChannel {

		@Test
		@DisplayName("WEB+PUSH+EMAIL 채널 이벤트를 수신하면 각 채널 broadcast를 모두 호출한다")
		void allChannels_callsAllBroadcasts() {
			listener.onAdminBroadcastRequested(event(
				Set.of(NotificationChannel.WEB, NotificationChannel.PUSH, NotificationChannel.EMAIL),
				"notice-template"));

			then(notificationBroadcastService).should().broadcastWeb(any(), anyString(), anyString(), anyString());
			then(notificationBroadcastService).should().broadcastPush(any(), anyString(), anyString(), anyString());
			then(notificationBroadcastService).should().broadcastEmail(any(), anyString(), any());
		}

		@Test
		@DisplayName("한 채널이 실패해도 나머지 채널 broadcast는 계속 실행된다")
		void oneChannelFails_otherChannelsContinue() {
			willThrow(new RuntimeException("PUSH 실패"))
				.given(notificationBroadcastService).broadcastPush(any(), any(), any(), any());

			assertThatCode(() -> listener.onAdminBroadcastRequested(event(
				Set.of(NotificationChannel.WEB, NotificationChannel.PUSH), null)))
				.doesNotThrowAnyException();

			then(notificationBroadcastService).should().broadcastWeb(any(), anyString(), anyString(), anyString());
		}
	}
}
