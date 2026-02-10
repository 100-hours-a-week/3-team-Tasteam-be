package com.tasteam.domain.notification.entity;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "push_notification_target", uniqueConstraints = {
	@UniqueConstraint(name = "uq_fcm_token", columnNames = {"fcm_token"})
}, indexes = {
	@Index(name = "idx_push_target_member", columnList = "member_id, created_at")
})
public class PushNotificationTarget extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "fcm_token", nullable = false, length = 255)
	private String fcmToken;

	public static PushNotificationTarget create(Long memberId, String fcmToken) {
		return PushNotificationTarget.builder()
			.memberId(memberId)
			.fcmToken(fcmToken)
			.build();
	}
}
