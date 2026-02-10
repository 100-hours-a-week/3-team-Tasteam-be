package com.tasteam.domain.notification.dto.response;

import com.tasteam.domain.notification.entity.MemberNotificationPreference;
import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;

public record NotificationPreferenceResponse(
	NotificationChannel channel,
	NotificationType notificationType,
	Boolean isEnabled) {
	public static NotificationPreferenceResponse from(MemberNotificationPreference preference) {
		return new NotificationPreferenceResponse(
			preference.getChannel(),
			preference.getNotificationType(),
			preference.getIsEnabled());
	}

	public static NotificationPreferenceResponse ofDefault(NotificationChannel channel, NotificationType type) {
		return new NotificationPreferenceResponse(channel, type, getDefaultEnabled(channel));
	}

	private static Boolean getDefaultEnabled(NotificationChannel channel) {
		return switch (channel) {
			case WEB, PUSH -> true;
			case EMAIL, SMS -> false;
		};
	}
}
