package com.tasteam.domain.test.controller.docs;

import com.tasteam.domain.test.dto.DevMemberResponse;
import com.tasteam.global.dto.api.SuccessResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Test", description = "테스트용 예외 발생 API")
public interface WebhookTestControllerDocs {

	@Operation(summary = "비즈니스 예외 테스트", description = "BusinessException을 강제로 발생시킵니다.")
	@ApiResponse(responseCode = "500", description = "비즈니스 예외 발생")
	String testBusinessException();

	@Operation(summary = "시스템 예외 테스트", description = "RuntimeException을 강제로 발생시킵니다.")
	@ApiResponse(responseCode = "500", description = "시스템 예외 발생")
	String testSystemException();

	@Operation(summary = "개발용 고정 멤버 조회", description = "로컬 seed에 있는 고정 멤버와 가입 그룹을 조회합니다.")
	@ApiResponse(responseCode = "200", description = "조회 성공")
	SuccessResponse<DevMemberResponse> getDevMember();
}
