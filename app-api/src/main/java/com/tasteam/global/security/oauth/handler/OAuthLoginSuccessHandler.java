package com.tasteam.global.security.oauth.handler;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.tasteam.domain.auth.entity.RefreshToken;
import com.tasteam.domain.auth.store.RefreshTokenStore;
import com.tasteam.global.security.jwt.common.RefreshTokenHasher;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.oauth.cookie.OAuth2CookieProvider;
import com.tasteam.global.security.oauth.dto.CustomOAuthUserDetails;
import com.tasteam.global.security.oauth.repository.HttpCookieOAuth2AuthorizationRequestRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * OAuth2 로그인 성공 핸들러
 */
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtTokenProvider jwtTokenProvider;
	private final JwtCookieProvider jwtCookieProvider;
	private final RefreshTokenStore refreshTokenStore;
	private final OAuth2CookieProvider oAuth2CookieProvider;
	private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		CustomOAuthUserDetails oAuth2User = (CustomOAuthUserDetails)authentication.getPrincipal();
		Long memberId = oAuth2User.getUid();

		String refreshToken = jwtTokenProvider.generateRefreshToken(memberId);
		jwtCookieProvider.addRefreshTokenCookie(response, refreshToken);

		String tokenFamilyId = UUID.randomUUID().toString();
		refreshTokenStore.revokeByMemberId(memberId, Instant.now());
		refreshTokenStore.save(RefreshToken.issue(
			memberId,
			RefreshTokenHasher.hash(refreshToken),
			tokenFamilyId,
			jwtTokenProvider.getExpiration(refreshToken).toInstant()));

		String redirectUri = determineTargetUrl(request);
		authorizationRequestRepository.removeAuthorizationRequestCookies(response);

		getRedirectStrategy().sendRedirect(request, response, redirectUri);
	}

	private String determineTargetUrl(HttpServletRequest request) {
		return oAuth2CookieProvider.getRedirectUriCookie(request)
			.orElse("http://localhost:3000/oauth/callback");
	}
}
