package com.tasteam.domain.notification.entity;

import java.time.Instant;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification", indexes = {
	@Index(name = "idx_notification_member_id", columnList = "member_id, id DESC"),
	@Index(name = "idx_notification_member_unread", columnList = "member_id, read_at, id")
})
public class Notification extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 20)
	private NotificationType notificationType;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "body", nullable = false, length = 500)
	private String body;

	@Column(name = "deep_link", length = 500)
	private String deepLink;

	@Column(name = "event_id", length = 64, unique = true)
	private String eventId;

	@Column(name = "read_at")
	private Instant readAt;

	public static Notification create(Long memberId, NotificationType type, String title, String body,
		String deepLink) {
		return Notification.builder()
			.memberId(memberId)
			.notificationType(type)
			.title(title)
			.body(body)
			.deepLink(deepLink)
			.build();
	}

	public static Notification create(String eventId, Long memberId, NotificationType type, String title, String body,
		String deepLink) {
		return Notification.builder()
			.eventId(eventId)
			.memberId(memberId)
			.notificationType(type)
			.title(title)
			.body(body)
			.deepLink(deepLink)
			.build();
	}

	public boolean markAsRead(Instant readAt) {
		if (this.readAt != null) {
			return false;
		}
		this.readAt = readAt;
		return true;
	}

	public boolean isRead() {
		return this.readAt != null;
	}
}
