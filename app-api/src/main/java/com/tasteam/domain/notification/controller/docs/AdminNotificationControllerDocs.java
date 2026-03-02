package com.tasteam.domain.notification.controller.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import com.tasteam.domain.notification.dto.request.AdminBroadcastEmailRequest;
import com.tasteam.domain.notification.dto.request.AdminBroadcastPushRequest;
import com.tasteam.domain.notification.dto.request.AdminPushNotificationRequest;
import com.tasteam.domain.notification.dto.response.AdminBroadcastResultResponse;
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

	@Operation(summary = "푸시 알림 브로드캐스트", description = "지정한 NotificationType의 PUSH 채널을 허용한 모든 회원에게 FCM 알림을 일괄 발송합니다.")
	ResponseEntity<SuccessResponse<AdminBroadcastResultResponse>> broadcastPush(
		@RequestBody @Valid
		AdminBroadcastPushRequest request);

	@Operation(summary = "이메일 일괄 발송", description = "지정한 NotificationType의 EMAIL 채널을 허용한 모든 회원에게 템플릿 이메일을 일괄 발송합니다.")
	ResponseEntity<SuccessResponse<AdminBroadcastResultResponse>> broadcastEmail(
		@RequestBody @Valid
		AdminBroadcastEmailRequest request);
}
