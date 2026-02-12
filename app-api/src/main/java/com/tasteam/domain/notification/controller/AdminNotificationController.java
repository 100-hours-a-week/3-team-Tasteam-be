package com.tasteam.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.notification.dto.request.AdminPushNotificationRequest;
import com.tasteam.domain.notification.dto.response.AdminPushNotificationResponse;
import com.tasteam.domain.notification.service.FcmPushService;
import com.tasteam.global.dto.api.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController {

	private final FcmPushService fcmPushService;

	@PostMapping("/push/test")
	public ResponseEntity<SuccessResponse<AdminPushNotificationResponse>> sendTestPush(
		@RequestBody @Valid
		AdminPushNotificationRequest request) {
		AdminPushNotificationResponse response = fcmPushService.sendToMember(
			request.memberId(),
			request.title(),
			request.body(),
			request.deepLink());
		return ResponseEntity.ok(SuccessResponse.success(response));
	}
}
