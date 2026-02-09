package com.tasteam.domain.notification.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.tasteam.domain.group.event.GroupMemberJoinedEvent;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

	private final NotificationService notificationService;

	@Async("notificationExecutor")
	@EventListener
	public void onGroupMemberJoined(GroupMemberJoinedEvent event) {
		try {
			notificationService.createNotification(
				event.memberId(),
				NotificationType.SYSTEM,
				"그룹 가입 완료",
				event.groupName() + " 그룹에 가입되었습니다.",
				"/groups/" + event.groupId());
			log.info("그룹 가입 알림 생성 완료. groupId={}, memberId={}", event.groupId(), event.memberId());
		} catch (Exception e) {
			log.error("그룹 가입 알림 생성 실패. groupId={}, memberId={}", event.groupId(), event.memberId(), e);
		}
	}
}
