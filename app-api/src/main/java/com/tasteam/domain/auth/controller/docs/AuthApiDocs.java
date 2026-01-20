package com.tasteam.domain.auth.controller.docs;

import com.tasteam.domain.auth.dto.response.RefreshTokenResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.RefreshToken;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "Auth", description = "인증 관련 API")
public interface AuthApiDocs {

	@Operation(summary = "액세스 토큰 갱신", description = "리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급받습니다.")
	SuccessResponse<RefreshTokenResponse> refreshToken(
			@RefreshToken
			String refreshToken,
			HttpServletResponse response);

	@Operation(summary = "로그아웃", description = "로그아웃하고 리프레시 토큰 쿠키를 무효화합니다. "
		+ "실제 처리는 Spring Security Filter에서 처리되며, "
		+ "이 엔드포인트는 Swagger 문서화용입니다.")
	SuccessResponse<Void> logout(
			HttpServletRequest request,
			HttpServletResponse response);

}
