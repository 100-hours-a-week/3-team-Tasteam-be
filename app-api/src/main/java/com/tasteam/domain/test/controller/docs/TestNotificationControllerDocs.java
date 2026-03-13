package com.tasteam.domain.test.controller.docs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import com.tasteam.domain.notification.dto.request.AdminMqTestNotificationRequest;
import com.tasteam.domain.notification.dto.response.AdminMqTestNotificationResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@SwaggerTagOrder(200)
@Tag(name = "Test - Notification", description = "[dev/local/stg 전용] 알림 MQ 파이프라인 테스트 API")
public interface TestNotificationControllerDocs {

	@Operation(summary = "MQ 파이프라인 테스트 알림 발송", description = "Outbox → MQ → Consumer 전체 파이프라인을 경유하여 특정 회원에게 알림을 발송합니다. "
		+ "Outbox에 등록 후 202 Accepted를 즉시 반환하며, Outbox Scanner 주기(30초)에 따라 실제 발송됩니다. "
		+ "MQ 비활성 상태(message-queue.enabled=false)이면 400을 반환합니다.")
	ResponseEntity<SuccessResponse<AdminMqTestNotificationResponse>> sendMqTestNotification(
		@RequestBody @Valid
		AdminMqTestNotificationRequest request);
}
