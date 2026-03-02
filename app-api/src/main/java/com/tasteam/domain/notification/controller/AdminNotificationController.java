package com.tasteam.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.notification.controller.docs.AdminNotificationControllerDocs;
import com.tasteam.domain.notification.dto.request.AdminBroadcastEmailRequest;
import com.tasteam.domain.notification.dto.request.AdminBroadcastPushRequest;
import com.tasteam.domain.notification.dto.request.AdminPushNotificationRequest;
import com.tasteam.domain.notification.dto.response.AdminBroadcastResultResponse;
import com.tasteam.domain.notification.dto.response.AdminPushNotificationResponse;
import com.tasteam.domain.notification.service.FcmPushService;
import com.tasteam.domain.notification.service.NotificationBroadcastService;
import com.tasteam.global.dto.api.SuccessResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationController implements AdminNotificationControllerDocs {

	private final FcmPushService fcmPushService;
	private final NotificationBroadcastService notificationBroadcastService;

	@Override
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

	@Override
	@PostMapping("/push/broadcast")
	public ResponseEntity<SuccessResponse<AdminBroadcastResultResponse>> broadcastPush(
		@RequestBody @Valid
		AdminBroadcastPushRequest request) {
		AdminBroadcastResultResponse response = notificationBroadcastService.broadcastPush(
			request.notificationType(), request.title(), request.body(), request.deepLink());
		return ResponseEntity.ok(SuccessResponse.success(response));
	}

	@Override
	@PostMapping("/email/broadcast")
	public ResponseEntity<SuccessResponse<AdminBroadcastResultResponse>> broadcastEmail(
		@RequestBody @Valid
		AdminBroadcastEmailRequest request) {
		AdminBroadcastResultResponse response = notificationBroadcastService.broadcastEmail(
			request.notificationType(), request.templateKey(), request.variables());
		return ResponseEntity.ok(SuccessResponse.success(response));
	}
}
