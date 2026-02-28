package com.tasteam.domain.notification.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.tasteam.domain.group.event.GroupMemberJoinedEvent;
import com.tasteam.domain.group.event.GroupRequestReviewedEvent;
import com.tasteam.domain.group.event.GroupRequestSubmittedEvent;
import com.tasteam.domain.member.event.MemberRegisteredEvent;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.outbox.NotificationOutboxService;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tasteam.message-queue", name = "enabled", havingValue = "true")
public class NotificationDomainEventListener {

	private final NotificationOutboxService outboxService;

	@Value("${tasteam.email.app-url:https://tasteam.co.kr}")
	private String appUrl;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onGroupMemberJoined(GroupMemberJoinedEvent event) {
		try {
			NotificationRequestedPayload payload = new NotificationRequestedPayload(
				UUID.randomUUID().toString(),
				"GroupMemberJoinedEvent",
				event.memberId(),
				NotificationType.SYSTEM,
				List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
				"group-joined",
				Map.of(
					"title", "그룹 가입 완료",
					"body", event.groupName() + " 그룹에 가입되었습니다.",
					"groupId", event.groupId(),
					"groupName", event.groupName()),
				"/groups/" + event.groupId(),
				event.joinedAt());
			outboxService.enqueue(payload);
		} catch (Exception ex) {
			log.error("GroupMemberJoinedEvent 알림 아웃박스 등록 실패. groupId={}, memberId={}",
				event.groupId(), event.memberId(), ex);
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onGroupRequestSubmitted(GroupRequestSubmittedEvent event) {
		try {
			NotificationRequestedPayload payload = new NotificationRequestedPayload(
				UUID.randomUUID().toString(),
				"GroupRequestSubmittedEvent",
				event.ownerId(),
				NotificationType.SYSTEM,
				List.of(NotificationChannel.WEB, NotificationChannel.PUSH),
				"group-request-submitted",
				Map.of(
					"title", "새 가입 신청",
					"body", event.groupName() + " 그룹에 새로운 가입 신청이 있습니다.",
					"groupId", event.groupId(),
					"groupName", event.groupName(),
					"applicantMemberId", event.applicantMemberId()),
				"/groups/" + event.groupId() + "/requests",
				event.submittedAt());
			outboxService.enqueue(payload);
		} catch (Exception ex) {
			log.error("GroupRequestSubmittedEvent 알림 아웃박스 등록 실패. groupId={}, ownerId={}",
				event.groupId(), event.ownerId(), ex);
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onGroupRequestReviewed(GroupRequestReviewedEvent event) {
		try {
			boolean approved = event.result() == GroupRequestReviewedEvent.ReviewResult.APPROVED;
			String templateKey = approved ? "group-request-approved" : "group-request-rejected";
			String title = approved ? "가입 승인됨" : "가입 거절됨";
			String body = approved
				? event.groupName() + " 그룹 가입이 승인되었습니다."
				: event.groupName() + " 그룹 가입이 거절되었습니다.";

			Map<String, Object> vars = new HashMap<>();
			vars.put("subject", title);
			vars.put("title", title);
			vars.put("body", body);
			vars.put("groupId", event.groupId());
			vars.put("groupName", event.groupName());
			vars.put("result", event.result().name());
			vars.put("appUrl", appUrl);
			if (event.reason() != null) {
				vars.put("reason", event.reason());
			}

			NotificationRequestedPayload payload = new NotificationRequestedPayload(
				UUID.randomUUID().toString(),
				"GroupRequestReviewedEvent",
				event.applicantMemberId(),
				NotificationType.SYSTEM,
				List.of(NotificationChannel.WEB, NotificationChannel.PUSH, NotificationChannel.EMAIL),
				templateKey,
				Map.copyOf(vars),
				"/groups/" + event.groupId(),
				event.reviewedAt());
			outboxService.enqueue(payload);
		} catch (Exception ex) {
			log.error("GroupRequestReviewedEvent 알림 아웃박스 등록 실패. groupId={}, applicantMemberId={}",
				event.groupId(), event.applicantMemberId(), ex);
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onMemberRegistered(MemberRegisteredEvent event) {
		try {
			NotificationRequestedPayload payload = new NotificationRequestedPayload(
				UUID.randomUUID().toString(),
				"MemberRegisteredEvent",
				event.memberId(),
				NotificationType.SYSTEM,
				List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH),
				"member-welcome",
				Map.of(
					"subject", "Tasteam에 오신 것을 환영합니다!",
					"nickname", event.nickname(),
					"appUrl", appUrl),
				"/home",
				event.registeredAt());
			outboxService.enqueue(payload);
		} catch (Exception ex) {
			log.error("MemberRegisteredEvent 알림 아웃박스 등록 실패. memberId={}", event.memberId(), ex);
		}
	}
}
