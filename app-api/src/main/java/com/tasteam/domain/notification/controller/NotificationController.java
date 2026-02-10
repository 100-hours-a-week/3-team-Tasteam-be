package com.tasteam.domain.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.notification.controller.docs.NotificationControllerDocs;
import com.tasteam.domain.notification.dto.request.NotificationPreferenceUpdateRequest;
import com.tasteam.domain.notification.dto.request.PushNotificationTargetRegisterRequest;
import com.tasteam.domain.notification.dto.response.NotificationPreferenceResponse;
import com.tasteam.domain.notification.dto.response.NotificationResponse;
import com.tasteam.domain.notification.dto.response.UnreadCountResponse;
import com.tasteam.domain.notification.service.NotificationPreferenceService;
import com.tasteam.domain.notification.service.NotificationService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.dto.pagination.OffsetPageResponse;
import com.tasteam.global.security.jwt.annotation.CurrentUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/members/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class NotificationController implements NotificationControllerDocs {

	private final NotificationService notificationService;
	private final NotificationPreferenceService preferenceService;

	@Override
	@GetMapping("/notifications")
	public SuccessResponse<OffsetPageResponse<NotificationResponse>> getNotifications(
		@CurrentUser
		Long memberId,
		@RequestParam(defaultValue = "0")
		int page,
		@RequestParam(defaultValue = "10") @Max(100)
		int size) {
		return SuccessResponse.success(notificationService.getNotifications(memberId, page, size));
	}

	@Override
	@PatchMapping("/notifications/{id}")
	public ResponseEntity<Void> markAsRead(
		@CurrentUser
		Long memberId,
		@PathVariable @Positive
		Long id) {
		notificationService.markAsRead(memberId, id);
		return ResponseEntity.noContent().build();
	}

	@Override
	@PatchMapping("/notifications")
	public ResponseEntity<Void> markAllAsRead(@CurrentUser
	Long memberId) {
		notificationService.markAllAsRead(memberId);
		return ResponseEntity.noContent().build();
	}

	@Override
	@GetMapping("/notifications/unread")
	public SuccessResponse<UnreadCountResponse> getUnreadCount(@CurrentUser
	Long memberId) {
		return SuccessResponse.success(notificationService.getUnreadCount(memberId));
	}

	@Override
	@GetMapping("/notification-preferences")
	public SuccessResponse<List<NotificationPreferenceResponse>> getPreferences(@CurrentUser
	Long memberId) {
		return SuccessResponse.success(preferenceService.getPreferences(memberId));
	}

	@Override
	@PatchMapping("/notification-preferences")
	public ResponseEntity<Void> updatePreferences(
		@CurrentUser
		Long memberId,
		@RequestBody @Valid
		NotificationPreferenceUpdateRequest request) {
		preferenceService.updatePreferences(memberId, request);
		return ResponseEntity.noContent().build();
	}

	@Override
	@PostMapping("/push-notification-targets")
	public ResponseEntity<Void> registerPushTarget(
		@CurrentUser
		Long memberId,
		@RequestBody @Valid
		PushNotificationTargetRegisterRequest request) {
		preferenceService.registerPushTarget(memberId, request);
		return ResponseEntity.noContent().build();
	}
}
