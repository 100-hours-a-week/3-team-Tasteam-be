package com.tasteam.domain.notification.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.tasteam.domain.notification.dto.response.AdminPushNotificationResponse;
import com.tasteam.domain.notification.entity.PushNotificationTarget;
import com.tasteam.domain.notification.repository.PushNotificationTargetRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FcmPushService {

	private static final int FCM_BATCH_SIZE = 500;

	private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;
	private final PushNotificationTargetRepository pushTargetRepository;

	@Transactional
	public AdminPushNotificationResponse sendToMember(Long memberId, String title, String body, String deepLink) {
		FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
		if (firebaseMessaging == null) {
			throw new IllegalStateException("FCM is not configured.");
		}

		List<PushNotificationTarget> targets = pushTargetRepository.findAllByMemberId(memberId);
		if (targets.isEmpty()) {
			return new AdminPushNotificationResponse(0, 0, 0);
		}

		List<String> tokens = targets.stream()
			.map(PushNotificationTarget::getFcmToken)
			.toList();
		List<Message> messages = tokens.stream()
			.map(token -> buildMessage(token, title, body, deepLink))
			.toList();

		int successCount = 0;
		int failureCount = 0;
		int invalidTokenCount = 0;

		for (int start = 0; start < messages.size(); start += FCM_BATCH_SIZE) {
			int end = Math.min(start + FCM_BATCH_SIZE, messages.size());
			List<Message> batch = messages.subList(start, end);
			try {
				BatchResponse response = firebaseMessaging.sendEach(batch);
				successCount += response.getSuccessCount();
				failureCount += response.getFailureCount();
				invalidTokenCount += handleFailures(response, tokens.subList(start, end));
			} catch (FirebaseMessagingException ex) {
				failureCount += batch.size();
			}
		}

		return new AdminPushNotificationResponse(successCount, failureCount, invalidTokenCount);
	}

	@Transactional
	public AdminPushNotificationResponse sendToMembers(Set<Long> memberIds, String title, String body,
		String deepLink) {
		if (memberIds == null || memberIds.isEmpty()) {
			return new AdminPushNotificationResponse(0, 0, 0);
		}

		FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
		if (firebaseMessaging == null) {
			throw new IllegalStateException("FCM is not configured.");
		}

		List<PushNotificationTarget> targets = pushTargetRepository.findAllByMemberIdIn(memberIds);
		if (targets.isEmpty()) {
			return new AdminPushNotificationResponse(0, 0, 0);
		}

		List<String> tokens = targets.stream()
			.map(PushNotificationTarget::getFcmToken)
			.distinct()
			.toList();
		List<Message> messages = tokens.stream()
			.map(token -> buildMessage(token, title, body, deepLink))
			.toList();

		int successCount = 0;
		int failureCount = 0;
		int invalidTokenCount = 0;

		for (int start = 0; start < messages.size(); start += FCM_BATCH_SIZE) {
			int end = Math.min(start + FCM_BATCH_SIZE, messages.size());
			List<Message> batch = messages.subList(start, end);
			try {
				BatchResponse response = firebaseMessaging.sendEach(batch);
				successCount += response.getSuccessCount();
				failureCount += response.getFailureCount();
				invalidTokenCount += handleFailures(response, tokens.subList(start, end));
			} catch (FirebaseMessagingException ex) {
				failureCount += batch.size();
			}
		}

		return new AdminPushNotificationResponse(successCount, failureCount, invalidTokenCount);
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

	private int handleFailures(BatchResponse response, List<String> tokens) {
		int invalidCount = 0;
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
				String token = tokens.get(i);
				pushTargetRepository.deleteByFcmToken(token);
				invalidCount++;
			}
		}
		return invalidCount;
	}
}
