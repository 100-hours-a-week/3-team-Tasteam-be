package com.tasteam.domain.notification.controller.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import com.tasteam.domain.notification.dto.request.AdminPushNotificationRequest;
import com.tasteam.domain.notification.dto.response.AdminPushNotificationResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@SwaggerTagOrder(150)
@Tag(name = "Admin - Notification", description = "어드민 알림 관리 API")
public interface AdminNotificationControllerDocs {

	@Operation(summary = "푸시 알림 테스트 발송", description = "특정 회원에게 FCM 푸시 알림을 테스트 발송합니다.")
	ResponseEntity<SuccessResponse<AdminPushNotificationResponse>> sendTestPush(
		@RequestBody @Valid
		AdminPushNotificationRequest request);
}
