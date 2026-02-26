package com.tasteam.domain.auth.controller.docs;

import org.springframework.validation.annotation.Validated;

import com.tasteam.domain.auth.dto.request.LocalAuthTokenRequest;
import com.tasteam.domain.auth.dto.response.LocalAuthTokenResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;
import com.tasteam.global.swagger.error.code.auth.AuthSwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;

@SwaggerTagOrder(5)
@Tag(name = "Auth", description = "인증 관련 API")
public interface LocalAuthControllerDocs {

	@Operation(summary = "로컬 개발용 토큰 발급", description = "로컬 프로필에서만 사용 가능한 토큰 발급 API입니다.")
	@RequestBody(required = true, content = @Content(schema = @Schema(implementation = LocalAuthTokenRequest.class)))
	@ApiResponse(responseCode = "200", description = "토큰 발급 성공", content = @Content(schema = @Schema(implementation = LocalAuthTokenResponse.class)))
	@CustomErrorResponseDescription(value = AuthSwaggerErrorResponseDescription.class, group = "LOCAL_TOKEN_ISSUE")
	SuccessResponse<LocalAuthTokenResponse> issueLocalToken(
		@Validated
		LocalAuthTokenRequest request,
		HttpServletResponse response);

	@Operation(summary = "AI 서버 헬스 체크", description = "로컬 프로필에서만 사용하는 AI 서버 연결 테스트 API입니다.")
	@ApiResponse(responseCode = "200", description = "헬스 체크 성공")
	SuccessResponse<Void> checkAiHealth();
}
