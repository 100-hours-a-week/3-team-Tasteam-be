package com.tasteam.domain.notification.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.notification.dto.response.NotificationResponse;
import com.tasteam.domain.notification.dto.response.UnreadCountResponse;
import com.tasteam.domain.notification.entity.Notification;
import com.tasteam.domain.notification.entity.NotificationType;
import com.tasteam.domain.notification.repository.NotificationRepository;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.dto.pagination.OffsetPagination;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.NotificationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;

	@Transactional(readOnly = true)
	public OffsetPageResponse<NotificationResponse> getNotifications(Long memberId, int page, int size) {
		Page<Notification> notificationPage = notificationRepository.findAllByMemberIdOrderByIdDesc(
			memberId, PageRequest.of(page, size));

		return new OffsetPageResponse<>(
			notificationPage.getContent().stream()
				.map(NotificationResponse::from)
				.toList(),
			new OffsetPagination(
				notificationPage.getNumber(),
				notificationPage.getSize(),
				notificationPage.getTotalPages(),
				(int)notificationPage.getTotalElements()));
	}

	@Transactional
	public void markAsRead(Long memberId, Long notificationId) {
		Notification notification = notificationRepository.findByIdAndMemberId(notificationId, memberId)
			.orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

		notification.markAsRead(Instant.now());
	}

	@Transactional
	public void markAllAsRead(Long memberId) {
		notificationRepository.markAllAsRead(memberId, Instant.now());
	}

	@Transactional(readOnly = true)
	public UnreadCountResponse getUnreadCount(Long memberId) {
		long count = notificationRepository.countByMemberIdAndReadAtIsNull(memberId);
		return new UnreadCountResponse((int)count);
	}

	@Transactional
	public Notification createNotification(
		Long memberId,
		NotificationType type,
		String title,
		String body,
		String deepLink) {
		return notificationRepository.save(Notification.create(memberId, type, title, body, deepLink));
	}

	@Transactional
	public Notification createNotification(
		String eventId,
		Long memberId,
		NotificationType type,
		String title,
		String body,
		String deepLink) {
		return notificationRepository.save(Notification.create(eventId, memberId, type, title, body, deepLink));
	}
}
