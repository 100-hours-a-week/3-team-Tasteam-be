package com.tasteam.domain.notification.event;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.tasteam.domain.chat.dto.ChatRoomMemberSnapshot;
import com.tasteam.domain.chat.event.ChatMessageSentEvent;
import com.tasteam.domain.chat.presence.ChatRoomPresenceRegistry;
import com.tasteam.domain.chat.repository.ChatRoomMemberRepository;
import com.tasteam.domain.chat.type.ChatMessageType;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.service.FcmPushService;
import com.tasteam.domain.notification.service.NotificationPreferenceService;
import com.tasteam.domain.notification.service.NotificationService;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNotificationEventListener {

	private static final Duration RECENT_READ_WINDOW = Duration.ofSeconds(10);
	private static final int PREVIEW_LIMIT = 50;
	private static final String FILE_MESSAGE_PREVIEW = "파일을 보냈습니다.";

	private final ChatRoomMemberRepository chatRoomMemberRepository;
	private final NotificationService notificationService;
	private final NotificationPreferenceService preferenceService;
	private final ChatRoomPresenceRegistry presenceRegistry;
	private final FcmPushService fcmPushService;
	private final MeterRegistry meterRegistry;

	@Async("notificationExecutor")
	@EventListener
	public void onChatMessageSent(ChatMessageSentEvent event) {
		if (event.messageType() == ChatMessageType.SYSTEM) {
			return;
		}

		List<ChatRoomMemberSnapshot> snapshots = chatRoomMemberRepository
			.findActiveMemberSnapshots(event.chatRoomId());
		if (snapshots.isEmpty()) {
			return;
		}

		Map<Long, ChatRoomMemberSnapshot> snapshotByMemberId = snapshots.stream()
			.collect(Collectors.toMap(ChatRoomMemberSnapshot::memberId, snapshot -> snapshot));

		List<Long> recipientIds = snapshots.stream()
			.map(ChatRoomMemberSnapshot::memberId)
			.filter(memberId -> !memberId.equals(event.senderId()))
			.toList();
		if (recipientIds.isEmpty()) {
			return;
		}

		String title = event.senderNickname() == null ? "새 메시지" : event.senderNickname();
		String body = buildPreview(event);
		String deepLink = "/chat-rooms/" + event.chatRoomId();

		for (Long memberId : recipientIds) {
			notificationService.createNotification(memberId, NotificationType.CHAT, title, body, deepLink);
			meterRegistry.counter("notification.chat.created.total").increment();
		}

		Map<Long, Boolean> pushEnabled = preferenceService.getEnabledMap(
			recipientIds, NotificationChannel.PUSH, NotificationType.CHAT);

		Instant now = Instant.now();
		List<Long> pushEligible = recipientIds.stream()
			.filter(memberId -> pushEnabled.getOrDefault(memberId, true))
			.toList();

		Set<Long> pushTargets = pushEligible.stream()
			.filter(memberId -> !isActiveInRoom(event.chatRoomId(), memberId, snapshotByMemberId, now))
			.collect(Collectors.toSet());

		int skipped = pushEligible.size() - pushTargets.size();
		if (skipped > 0) {
			meterRegistry.counter("notification.chat.push.skipped.online.total").increment(skipped);
		}

		if (pushTargets.isEmpty()) {
			return;
		}

		try {
			var response = fcmPushService.sendToMembers(pushTargets, title, body, deepLink);
			meterRegistry.counter("notification.chat.push.sent.total").increment(response.successCount());
		} catch (Exception ex) {
			log.error("Failed to send chat push. chatRoomId={}, messageId={}",
				event.chatRoomId(), event.messageId(), ex);
		}
	}

	private boolean isActiveInRoom(Long chatRoomId, Long memberId,
		Map<Long, ChatRoomMemberSnapshot> snapshotByMemberId, Instant now) {
		if (presenceRegistry.isMemberOnline(chatRoomId, memberId)) {
			return true;
		}

		ChatRoomMemberSnapshot snapshot = snapshotByMemberId.get(memberId);
		if (snapshot == null || snapshot.lastReadUpdatedAt() == null) {
			return false;
		}

		Duration since = Duration.between(snapshot.lastReadUpdatedAt(), now);
		return !since.isNegative() && since.compareTo(RECENT_READ_WINDOW) <= 0;
	}

	private String buildPreview(ChatMessageSentEvent event) {
		if (event.messageType() == ChatMessageType.FILE) {
			return FILE_MESSAGE_PREVIEW;
		}

		String content = event.preview();
		if (content == null) {
			return "";
		}
		return truncateToCodePoints(content, PREVIEW_LIMIT);
	}

	private String truncateToCodePoints(String value, int limit) {
		if (value == null) {
			return "";
		}
		int length = value.codePointCount(0, value.length());
		if (length <= limit) {
			return value;
		}
		int end = value.offsetByCodePoints(0, limit);
		return value.substring(0, end);
	}
}
