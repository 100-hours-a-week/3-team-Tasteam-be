package com.tasteam.infra.messagequeue.trace;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.global.dto.api.SuccessResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/mq-traces")
@PreAuthorize("hasRole('ADMIN')")
public class MessageQueueTraceAdminController {

	private static final int MAX_LIMIT = 200;

	private final MessageQueueTraceService traceService;

	@GetMapping
	@ResponseStatus(HttpStatus.OK)
	public SuccessResponse<List<MessageQueueTraceLogResponse>> findRecent(
		@RequestParam(required = false)
		String messageId,
		@RequestParam(defaultValue = "50")
		int limit) {
		int validatedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
		List<MessageQueueTraceLogResponse> response = traceService.findRecent(messageId, validatedLimit)
			.stream()
			.map(MessageQueueTraceLogResponse::from)
			.toList();
		return SuccessResponse.success(response);
	}
}
