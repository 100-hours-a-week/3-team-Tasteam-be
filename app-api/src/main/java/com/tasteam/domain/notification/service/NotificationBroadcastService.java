package com.tasteam.domain.notification.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.domain.notification.dto.response.AdminBroadcastResultResponse;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.entity.PushNotificationTarget;
import com.tasteam.domain.notification.repository.NotificationPreferenceRepository;
import com.tasteam.domain.notification.repository.PushNotificationTargetRepository;
import com.tasteam.global.notification.email.EmailSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationBroadcastService {

	private static final int FCM_BATCH_SIZE = 500;
	private static final int EMAIL_PAGE_SIZE = 200;

	private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
	private final PushNotificationTargetRepository pushTargetRepository;
	private final MemberRepository memberRepository;
	private final NotificationPreferenceRepository preferenceRepository;
	private final EmailSender emailSender;

	@Transactional
	public AdminBroadcastResultResponse broadcastPush(
		NotificationType notificationType, String title, String body, String deepLink) {

		FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
		if (firebaseMessaging == null) {
			throw new IllegalStateException("FCM is not configured.");
		}

		Set<Long> disabledMemberIds = preferenceRepository.findDisabledMemberIds(NotificationChannel.PUSH,
			notificationType);

		int page = 0;
		int totalTargets = 0, successCount = 0, failureCount = 0, skippedCount = 0;

		while (true) {
			List<PushNotificationTarget> targets = pushTargetRepository.findAll(PageRequest.of(page++, FCM_BATCH_SIZE))
				.getContent();
			if (targets.isEmpty()) {
				break;
			}

			List<PushNotificationTarget> allowed = targets.stream()
				.filter(t -> !disabledMemberIds.contains(t.getMemberId()))
				.toList();

			skippedCount += targets.size() - allowed.size();
			totalTargets += allowed.size();

			List<Message> messages = allowed.stream()
				.map(t -> buildMessage(t.getFcmToken(), title, body, deepLink))
				.toList();

			for (int start = 0; start < messages.size(); start += FCM_BATCH_SIZE) {
				int end = Math.min(start + FCM_BATCH_SIZE, messages.size());
				List<Message> batch = messages.subList(start, end);
				List<String> batchTokens = allowed.subList(start, end).stream()
					.map(PushNotificationTarget::getFcmToken)
					.toList();
				try {
					BatchResponse batchResponse = firebaseMessaging.sendAll(batch, false);
					successCount += batchResponse.getSuccessCount();
					failureCount += batchResponse.getFailureCount();
					handlePushFailures(batchResponse, batchTokens);
				} catch (FirebaseMessagingException ex) {
					failureCount += batch.size();
				}
			}
		}
		return new AdminBroadcastResultResponse(totalTargets, successCount, failureCount, skippedCount);
	}

	@Transactional(readOnly = true)
	public AdminBroadcastResultResponse broadcastEmail(
		NotificationType notificationType, String templateKey, Map<String, Object> variables) {

		Set<Long> disabledMemberIds = preferenceRepository.findDisabledMemberIds(NotificationChannel.EMAIL,
			notificationType);

		int page = 0;
		int totalTargets = 0, successCount = 0, failureCount = 0, skippedCount = 0;

		while (true) {
			List<Member> members = memberRepository.findAllWithEmail(PageRequest.of(page++, EMAIL_PAGE_SIZE))
				.getContent();
			if (members.isEmpty()) {
				break;
			}

			for (Member member : members) {
				if (disabledMemberIds.contains(member.getId())) {
					skippedCount++;
					continue;
				}
				totalTargets++;
				try {
					emailSender.sendTemplateEmail(member.getEmail(), templateKey, variables);
					successCount++;
				} catch (Exception e) {
					log.error("이메일 발송 실패. memberId={}, templateKey={}", member.getId(), templateKey, e);
					failureCount++;
				}
			}
		}
		return new AdminBroadcastResultResponse(totalTargets, successCount, failureCount, skippedCount);
	}

	private Message buildMessage(String token, String title, String body, String deepLink) {
		Message.Builder builder = Message.builder()
			.setToken(token)
			.setNotification(Notification.builder()
				.setTitle(title)
				.setBody(body)
				.build());

		if (deepLink != null && !deepLink.isBlank()) {
			builder.putData("deepLink", deepLink);
		}

		return builder.build();
	}

	private void handlePushFailures(BatchResponse response, List<String> tokens) {
		for (int i = 0; i < response.getResponses().size(); i++) {
			if (response.getResponses().get(i).isSuccessful()) {
				continue;
			}
			Exception exception = response.getResponses().get(i).getException();
			if (!(exception instanceof FirebaseMessagingException messagingException)) {
				continue;
			}
			MessagingErrorCode errorCode = messagingException.getMessagingErrorCode();
			if (errorCode == MessagingErrorCode.UNREGISTERED
				|| errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
				pushTargetRepository.deleteByFcmToken(tokens.get(i));
			}
		}
	}
}
