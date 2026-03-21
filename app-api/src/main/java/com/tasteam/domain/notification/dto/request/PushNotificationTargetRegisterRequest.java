package com.tasteam.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushNotificationTargetRegisterRequest(
	@NotBlank @Size(max = 64)
	String deviceId,
	@NotBlank @Size(max = 255)
	String fcmToken) {
}
