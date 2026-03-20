package com.tasteam.domain.notification.entity;

import com.tasteam.domain.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "member_notification_preference", uniqueConstraints = {
	@UniqueConstraint(name = "uq_preference", columnNames = {"member_id", "channel", "notification_type"})
})
public class MemberNotificationPreference extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 10)
	private NotificationChannel channel;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 20)
	private NotificationType notificationType;

	@Column(name = "is_enabled", nullable = false)
	private Boolean isEnabled;

	public static MemberNotificationPreference create(Long memberId, NotificationChannel channel,
		NotificationType type, Boolean isEnabled) {
		return MemberNotificationPreference.builder()
			.memberId(memberId)
			.channel(channel)
			.notificationType(type)
			.isEnabled(isEnabled)
			.build();
	}

	public void changeEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
}
