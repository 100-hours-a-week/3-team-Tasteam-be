package com.tasteam.global.security.jwt.logout;

import java.io.IOException;
import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.tasteam.domain.auth.store.RefreshTokenStore;
import com.tasteam.global.security.common.logout.LogoutAction;
import com.tasteam.global.security.jwt.common.RefreshTokenHasher;
import com.tasteam.global.security.jwt.provider.JwtCookieProvider;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 로그아웃 시 Refresh Token 패밀리를 무효화하고 쿠키를 제거한다.
 * <p>
 * 필요 시 `spring.security.features.jwt=false`로 비활성화할 수 있다.
 */
@Component
@RequiredArgsConstructor
@Order(0)
@ConditionalOnProperty(prefix = "spring.security.features", name = "jwt", havingValue = "true", matchIfMissing = true)
public class JwtRefreshTokenLogoutAction implements LogoutAction {

	private final JwtCookieProvider jwtCookieProvider;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenStore refreshTokenStore;

	@Override
	public void onLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		jwtCookieProvider.getRefreshTokenFromCookie(request)
			.ifPresent(token -> {
				String tokenHash = RefreshTokenHasher.hash(token);
				refreshTokenStore.findByTokenHash(tokenHash)
					.ifPresentOrElse(
						stored -> refreshTokenStore.revokeByTokenFamilyId(
							stored.getTokenFamilyId(), Instant.now()),
						() -> refreshTokenStore.revokeByMemberId(
							jwtTokenProvider.getUidFromToken(token), Instant.now()));
			});

		jwtCookieProvider.deleteRefreshTokenCookie(response);
	}
}
