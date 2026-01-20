package com.tasteam.global.security.jwt.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.auth.entity.RefreshToken;
import com.tasteam.domain.auth.store.RefreshTokenStore;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.security.jwt.common.RefreshTokenHasher;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.user.repository.UserRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * 토큰 리프레시 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
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
		RefreshToken storedToken = refreshTokenStore.findByTokenHash(tokenHash)
			.orElse(null);

		if (storedToken == null) {
			refreshTokenStore.revokeByMemberId(memberId, Instant.now());
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_REUSED);
		}

		if (storedToken.isRotated() || storedToken.isRevoked()) {
			refreshTokenStore.revokeByTokenFamilyId(storedToken.getTokenFamilyId(), Instant.now());
			throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_REUSED);
		}

		String newAccessToken = jwtTokenProvider.generateAccessToken(memberId, member.role().name());
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(memberId);

		Instant now = Instant.now();
		storedToken.rotate(now);
		refreshTokenStore.save(storedToken);

		RefreshToken rotatedToken = RefreshToken.issue(
			memberId,
			RefreshTokenHasher.hash(newRefreshToken),
			storedToken.getTokenFamilyId(),
			jwtTokenProvider.getExpiration(newRefreshToken).toInstant());
		refreshTokenStore.save(rotatedToken);

		return new TokenPair(newAccessToken, newRefreshToken);
	}

	public record TokenPair(String accessToken, String refreshToken) {
	}
}
