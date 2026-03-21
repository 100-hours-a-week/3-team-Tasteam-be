package com.tasteam.domain.notification.event;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.service.NotificationBroadcastService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBroadcastNotificationEventListener {

	private final NotificationBroadcastService notificationBroadcastService;

	@Async("notificationExecutor")
	@EventListener
	public void onAdminBroadcastRequested(AdminBroadcastRequestedEvent event) {
		for (NotificationChannel channel : event.channels()) {
			try {
				switch (channel) {
					case WEB -> notificationBroadcastService.broadcastWeb(
						event.notificationType(), event.title(), event.body(), event.deepLink());
					case PUSH -> notificationBroadcastService.broadcastPush(
						event.notificationType(), event.title(), event.body(), event.deepLink());
					case EMAIL -> {
						if (event.templateKey() == null || event.templateKey().isBlank()) {
							log.error("EMAIL 채널 브로드캐스트 실패: templateKey가 없습니다.");
							continue;
						}
						notificationBroadcastService.broadcastEmail(
							event.notificationType(), event.templateKey(), event.templateVariables());
					}
					default -> log.warn("지원하지 않는 알림 채널: {}", channel);
				}
				log.info("어드민 브로드캐스트 완료. channel={}, type={}", channel, event.notificationType());
			} catch (Exception e) {
				log.error("어드민 브로드캐스트 실패. channel={}, type={}", channel, event.notificationType(), e);
			}
		}
	}
}
