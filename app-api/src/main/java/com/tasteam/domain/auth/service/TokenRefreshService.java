package com.tasteam.domain.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.auth.entity.RefreshToken;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.security.jwt.common.RefreshTokenHasher;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.jwt.repository.RefreshTokenStore;
import com.tasteam.global.security.user.repository.UserRepositoryPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 토큰 리프레시 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TokenRefreshService {

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepositoryPort userRepositoryPort;
	private final RefreshTokenStore refreshTokenStore;

	/**
	 * 리프레시 토큰을 사용하여 새로운 액세스 토큰을 발급합니다.
	 *
	 * @param refreshToken 리프레시 토큰
	 * @return 새로운 액세스/리프레시 토큰 쌍
	 * @throws BusinessException 리프레시 토큰이 유효하지 않거나 만료된 경우, 또는 사용자가 존재하지 않거나 비활성화된 경우 발생
	 */
	public TokenPair refreshTokens(String refreshToken) {
		if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
		}

		if (jwtTokenProvider.isTokenExpired(refreshToken)) {
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
		}

		Long memberId = jwtTokenProvider.getUidFromToken(refreshToken);
		var member = userRepositoryPort.findById(memberId)
			.orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

		if (!member.active()) {
			throw new BusinessException(MemberErrorCode.MEMBER_INACTIVE);
		}

		String tokenHash = RefreshTokenHasher.hash(refreshToken);
		RefreshToken storedToken = refreshTokenStore.findByTokenHashForUpdate(tokenHash)
			.orElse(null);

		if (storedToken == null) {
			// RTR 비활성화: 재사용 차단/회전 로직을 잠시 중지
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_REUSED);
		}

		// RTR 비활성화: rotated/revoked 체크를 생략

		String newAccessToken = jwtTokenProvider.generateAccessToken(memberId, member.role().name());
		// RTR 비활성화: refresh token은 그대로 사용
		return new TokenPair(newAccessToken, refreshToken);
	}

	public record TokenPair(String accessToken, String refreshToken) {
	}
}
