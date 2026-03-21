package com.tasteam.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminPushNotificationRequest(
	@NotNull
	Long memberId,
	@NotBlank @Size(max = 100)
	String title,
	@NotBlank @Size(max = 1000)
	String body,
	@Size(max = 500)
	String deepLink) {
}
