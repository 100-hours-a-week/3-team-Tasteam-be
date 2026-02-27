package com.tasteam.domain.notification.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.group.event.GroupMemberJoinedEvent;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.NotificationService;

@UnitTest
@DisplayName("NotificationEventListener")
class NotificationEventListenerTest {

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private NotificationEventListener notificationEventListener;

	@Test
	@DisplayName("그룹 가입 이벤트 수신 시 올바른 파라미터로 알림을 생성한다")
	void onGroupMemberJoined_createsNotificationWithCorrectParams() {
		GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(1L, 2L, "스터디", Instant.now());

		notificationEventListener.onGroupMemberJoined(event);

		then(notificationService).should().createNotification(
			2L,
			NotificationType.SYSTEM,
			"그룹 가입 완료",
			"스터디 그룹에 가입되었습니다.",
			"/groups/1");
	}

	@Test
	@DisplayName("알림 생성 실패해도 예외가 전파되지 않는다")
	void onGroupMemberJoined_whenServiceThrows_doesNotPropagate() {
		GroupMemberJoinedEvent event = new GroupMemberJoinedEvent(1L, 2L, "스터디", Instant.now());
		willThrow(new RuntimeException("알림 생성 실패"))
			.given(notificationService)
			.createNotification(anyLong(), any(), any(), any(), any());

		assertThatCode(() -> notificationEventListener.onGroupMemberJoined(event))
			.doesNotThrowAnyException();
	}
}
