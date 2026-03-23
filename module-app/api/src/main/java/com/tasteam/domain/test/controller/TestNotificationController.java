package com.tasteam.domain.test.controller;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.notification.dto.request.AdminMqTestNotificationRequest;
import com.tasteam.domain.notification.dto.response.AdminMqTestNotificationResponse;
import com.tasteam.domain.notification.outbox.NotificationOutboxService;
import com.tasteam.domain.notification.payload.NotificationRequestedPayload;
import com.tasteam.domain.test.controller.docs.TestNotificationControllerDocs;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.NotificationErrorCode;
import com.tasteam.infra.messagequeue.MessageQueueProperties;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Profile({"dev", "local", "stg"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/test/notifications")
public class TestNotificationController implements TestNotificationControllerDocs {

	private final NotificationOutboxService outboxService;
	private final MessageQueueProperties messageQueueProperties;

	@Override
	@PostMapping("/mq")
	public ResponseEntity<SuccessResponse<AdminMqTestNotificationResponse>> sendMqTestNotification(
		@RequestBody @Valid
		AdminMqTestNotificationRequest request) {
		if (!messageQueueProperties.isEnabled()) {
			throw new BusinessException(NotificationErrorCode.MESSAGE_QUEUE_DISABLED);
		}
		String eventId = UUID.randomUUID().toString();
		Map<String, Object> vars = request.templateVariables() != null
			? request.templateVariables()
			: Map.of("title", request.title(), "body", request.body());
		NotificationRequestedPayload payload = new NotificationRequestedPayload(
			eventId,
			"TestMqNotification",
			request.recipientId(),
			request.notificationType(),
			request.channels(),
			null,
			vars,
			request.deepLink(),
			Instant.now());
		outboxService.enqueue(payload);
		return ResponseEntity.accepted().body(SuccessResponse.success(
			new AdminMqTestNotificationResponse(
				eventId,
				"Outbox에 등록되었습니다. Outbox Scanner 주기(최대 30초)에 따라 MQ → Consumer 순으로 처리됩니다.")));
	}
}
