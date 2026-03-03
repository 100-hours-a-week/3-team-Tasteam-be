package com.tasteam.domain.auth.controller.docs;

import com.tasteam.domain.auth.dto.response.RefreshTokenResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.RefreshToken;
import com.tasteam.global.swagger.annotation.CustomErrorResponseDescription;
import com.tasteam.global.swagger.annotation.SwaggerTagOrder;
import com.tasteam.global.swagger.error.code.auth.AuthSwaggerErrorResponseDescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SwaggerTagOrder(5)
@Tag(name = "Auth", description = "인증 관련 API")
public interface AuthApiDocs {

	@Operation(summary = "액세스 토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
	@ApiResponse(responseCode = "200", description = "토큰 갱신 성공", content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class)))
	@CustomErrorResponseDescription(value = AuthSwaggerErrorResponseDescription.class, group = "AUTH_TOKEN_REFRESH")
	SuccessResponse<RefreshTokenResponse> refreshToken(
		@RefreshToken
		String refreshToken,
		HttpServletResponse response);

	@Operation(summary = "로그아웃", description = "로그아웃하고 리프레시 토큰 쿠키를 무효화합니다. "
		+ "실제 처리는 Spring Security Filter에서 처리되며, "
		+ "이 엔드포인트는 Swagger 문서화용입니다.")
	@ApiResponse(responseCode = "200", description = "로그아웃 성공")
	SuccessResponse<Void> logout(
		HttpServletRequest request,
		HttpServletResponse response);

}
