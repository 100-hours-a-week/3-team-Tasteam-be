package com.tasteam.domain.notification.dispatch;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.notification.consumer.ConsumedNotificationEventJdbcRepository;
import com.tasteam.domain.notification.entity.MemberNotificationPreference;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;
import com.tasteam.domain.notification.repository.NotificationPreferenceRepository;
import com.tasteam.domain.notification.service.FcmPushService;
import com.tasteam.domain.notification.service.NotificationService;
import com.tasteam.infra.email.EmailSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class NotificationDispatcher {

	private static final String CONSUMER_GROUP = "cg.notification.processor.v1";
	private static final String STREAM_KEY = "evt.notification.v1";

	private final ConsumedNotificationEventJdbcRepository consumedEventRepository;
	private final NotificationPreferenceRepository preferenceRepository;
	private final NotificationService notificationService;
	private final FcmPushService fcmPushService;
	private final EmailSender emailSender;
	private final MemberRepository memberRepository;
	private final FcmCircuitBreaker fcmCircuitBreaker;
	private final EmailCircuitBreaker emailCircuitBreaker;

	public void dispatch(NotificationRequestedPayload payload) {
		boolean inserted = consumedEventRepository.tryInsert(CONSUMER_GROUP, payload.eventId(), STREAM_KEY);
		if (!inserted) {
			log.debug("중복 알림 이벤트 스킵. eventId={}", payload.eventId());
			return;
		}

		List<MemberNotificationPreference> prefs = preferenceRepository
			.findAllByMemberIdAndChannelIn(payload.recipientId(), payload.channels());

		String title = extractString(payload.templateVariables(), "title", "");
		String body = extractString(payload.templateVariables(), "body", "");

		for (NotificationChannel channel : payload.channels()) {
			if (!isChannelEnabled(prefs, channel, payload.notificationType())) {
				continue;
			}
			try {
				dispatchToChannel(channel, payload, title, body);
			} catch (Exception ex) {
				log.error("채널 발송 실패. channel={}, eventId={}", channel, payload.eventId(), ex);
			}
		}
	}

	private void dispatchToChannel(NotificationChannel channel, NotificationRequestedPayload payload,
		String title, String body) {
		switch (channel) {
			case WEB -> notificationService.createNotification(
				payload.eventId(),
				payload.recipientId(),
				payload.notificationType(),
				title,
				body,
				payload.deepLink());
			case PUSH -> dispatchPush(payload.recipientId(), title, body, payload.deepLink());
			case EMAIL -> dispatchEmail(payload.recipientId(), payload.templateKey(),
				payload.templateVariables());
			default -> log.warn("지원하지 않는 채널. channel={}", channel);
		}
	}

	private void dispatchPush(Long recipientId, String title, String body, String deepLink) {
		if (!fcmCircuitBreaker.allowRequest()) {
			log.warn("FCM Circuit Breaker OPEN. PUSH 발송 스킵. recipientId={}", recipientId);
			return;
		}
		try {
			fcmPushService.sendToMember(recipientId, title, body, deepLink);
			fcmCircuitBreaker.recordSuccess();
		} catch (Exception ex) {
			fcmCircuitBreaker.recordFailure();
			throw ex;
		}
	}

	private void dispatchEmail(Long recipientId, String templateKey,
		Map<String, Object> variables) {
		if (!emailCircuitBreaker.allowRequest()) {
			log.warn("Email Circuit Breaker OPEN. EMAIL 발송 스킵. recipientId={}", recipientId);
			return;
		}
		memberRepository.findById(recipientId).ifPresentOrElse(member -> {
			String email = member.getEmail();
			if (email == null || email.isBlank()) {
				return;
			}
			try {
				emailSender.sendTemplateEmail(email, templateKey, variables);
				emailCircuitBreaker.recordSuccess();
			} catch (Exception ex) {
				emailCircuitBreaker.recordFailure();
				log.error("이메일 발송 실패. recipientId={}", recipientId, ex);
			}
		}, () -> log.warn("이메일 발송 대상 멤버 없음. recipientId={}", recipientId));
	}

	private boolean isChannelEnabled(List<MemberNotificationPreference> prefs, NotificationChannel channel,
		NotificationType type) {
		return prefs.stream()
			.filter(p -> p.getChannel() == channel && p.getNotificationType() == type)
			.findFirst()
			.map(MemberNotificationPreference::getIsEnabled)
			.orElse(true);
	}

	private String extractString(Map<String, Object> vars, String key, String defaultValue) {
		if (vars == null) {
			return defaultValue;
		}
		Object value = vars.get(key);
		return value instanceof String s ? s : defaultValue;
	}
}
