package com.tasteam.domain.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tasteam.domain.auth.controller.docs.AuthApiDocs;
import com.tasteam.domain.auth.dto.response.RefreshTokenResponse;
import com.tasteam.global.dto.api.SuccessResponse;
import com.tasteam.global.security.jwt.annotation.RefreshToken;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;
import com.tasteam.global.security.jwt.service.TokenRefreshService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthApiDocs {

	private final TokenRefreshService tokenRefreshService;
	private final JwtCookieProvider jwtCookieProvider;

	@PostMapping("/token/refresh")
	public SuccessResponse<RefreshTokenResponse> refreshToken(@RefreshToken
	String refreshToken,
		HttpServletResponse servletResponse) {
		TokenRefreshService.TokenPair tokenPair = tokenRefreshService.refreshTokens(refreshToken);
		jwtCookieProvider.addRefreshTokenCookie(servletResponse, tokenPair.refreshToken());

		RefreshTokenResponse response = new RefreshTokenResponse(tokenPair.accessToken());

		return SuccessResponse.success(response);
	}

	@PostMapping("/logout")
	public SuccessResponse<Void> logout(
		HttpServletRequest request,
		HttpServletResponse response) {
		throw new UnsupportedOperationException("해당 엔드포인트는 Security 필터에서 처리됩니다");
	}

}
