package com.tasteam.domain.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.auth.controller.docs.DevAuthControllerDocs;
import com.tasteam.domain.auth.dto.request.LocalAuthTokenRequest;
import com.tasteam.domain.auth.dto.response.DevAuthTokenResponse;
import com.tasteam.domain.auth.service.DevAuthTokenService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 관련 API")
@Profile({"local", "dev"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class DevAuthController implements DevAuthControllerDocs {

	private final DevAuthTokenService devAuthTokenService;
	private final JwtCookieProvider jwtCookieProvider;

	@Operation(summary = "로컬 개발용 토큰 발급", description = "로컬 프로필에서만 사용 가능한 토큰 발급 API입니다.")
	@PostMapping("/token")
	public SuccessResponse<DevAuthTokenResponse> issueLocalToken(
		@Valid @RequestBody
		LocalAuthTokenRequest request,
		HttpServletResponse response) {
		DevAuthTokenService.TokenPair tokenPair = devAuthTokenService.issueTokens(
			request.email(),
			request.nickname());

		jwtCookieProvider.addRefreshTokenCookie(response, tokenPair.refreshToken());
		DevAuthTokenResponse tokenResponse = new DevAuthTokenResponse(
			tokenPair.accessToken(),
			tokenPair.memberId());

		return SuccessResponse.success(tokenResponse);
	}
}
