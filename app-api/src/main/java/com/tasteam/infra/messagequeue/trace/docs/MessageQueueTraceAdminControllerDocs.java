package com.tasteam.infra.messagequeue.trace.docs;

import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;
import com.tasteam.infra.messagequeue.trace.MessageQueueTraceLogResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(170)
@Tag(name = "Admin - MQ", description = "어드민 메시지 큐 모니터링 API")
public interface MessageQueueTraceAdminControllerDocs {

	@Operation(summary = "최근 MQ 트레이스 조회", description = "최근 메시지 큐 처리 이력을 조회합니다.")
	SuccessResponse<List<MessageQueueTraceLogResponse>> findRecent(
		@Parameter(description = "메시지 ID 필터") @RequestParam(required = false)
		String messageId,
		@Parameter(description = "조회 개수 제한 (최대 200)", example = "50") @RequestParam
		int limit);
}
