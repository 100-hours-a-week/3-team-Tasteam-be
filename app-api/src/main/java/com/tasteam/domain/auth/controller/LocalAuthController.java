package com.tasteam.domain.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.auth.controller.docs.LocalAuthControllerDocs;
import com.tasteam.domain.auth.dto.request.LocalAuthTokenRequest;
import com.tasteam.domain.auth.dto.response.LocalAuthTokenResponse;
import com.tasteam.domain.auth.service.LocalAuthTokenService;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Profile("local")
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class LocalAuthController implements LocalAuthControllerDocs {

	private final LocalAuthTokenService localAuthTokenService;
	private final JwtCookieProvider jwtCookieProvider;

	@PostMapping("/token")
	public SuccessResponse<LocalAuthTokenResponse> issueLocalToken(
		@RequestBody
		LocalAuthTokenRequest request,
		HttpServletResponse response) {
		LocalAuthTokenService.TokenPair tokenPair = localAuthTokenService.issueTokens(
			request.email(),
			request.nickname());

		jwtCookieProvider.addRefreshTokenCookie(response, tokenPair.refreshToken());
		LocalAuthTokenResponse tokenResponse = new LocalAuthTokenResponse(
			tokenPair.accessToken(),
			tokenPair.memberId());

		return SuccessResponse.success(tokenResponse);
	}
}
