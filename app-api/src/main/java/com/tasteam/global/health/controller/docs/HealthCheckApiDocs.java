package com.tasteam.global.health.controller.docs;

import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.health.dto.response.HealthCheckResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "System", description = "시스템 상태 확인 API")
public interface HealthCheckApiDocs {

	@Operation(summary = "헬스 체크", description = "애플리케이션 상태를 빠르게 확인합니다.")
	@ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = HealthCheckResponse.class)))
	SuccessResponse<HealthCheckResponse> check();

}
