package com.tasteam.infra.messagequeue.docs;

import org.springframework.web.bind.annotation.RequestParam;

import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;
import com.tasteam.infra.messagequeue.UserActivityOutboxSummaryResponse;
import com.tasteam.infra.messagequeue.UserActivityReplayResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@SwaggerTagOrder(170)
@Tag(name = "Admin - MQ", description = "어드민 메시지 큐 모니터링 API")
public interface UserActivityOutboxAdminControllerDocs {

	@Operation(summary = "사용자 활동 아웃박스 요약 조회", description = "전송 대기/실패 아웃박스 메시지 현황을 조회합니다.")
	SuccessResponse<UserActivityOutboxSummaryResponse> getSummary();

	@Operation(summary = "사용자 활동 아웃박스 재전송", description = "전송 실패한 아웃박스 메시지를 재처리합니다.")
	SuccessResponse<UserActivityReplayResponse> replay(
		@Parameter(description = "재처리 개수 제한 (최대 500)", example = "100") @RequestParam
		int limit);
}
