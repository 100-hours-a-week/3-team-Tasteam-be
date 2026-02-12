package com.tasteam.domain.notification.dto.request;

import java.util.List;

import com.tasteam.domain.notification.entity.NotificationChannel;
import com.tasteam.domain.notification.entity.NotificationType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationPreferenceUpdateRequest(
	@NotNull @Size(min = 1) @Valid
	List<NotificationPreferenceItem> notificationPreferences) {
	public record NotificationPreferenceItem(
		@NotNull
		NotificationChannel channel,
		@NotNull
		NotificationType notificationType,
		@NotNull
		Boolean isEnabled) {
	}
}
