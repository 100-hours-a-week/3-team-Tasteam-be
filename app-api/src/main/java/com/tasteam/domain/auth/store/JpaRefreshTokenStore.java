package com.tasteam.domain.auth.store;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.tasteam.domain.auth.entity.RefreshToken;
import com.tasteam.domain.auth.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaRefreshTokenStore implements RefreshTokenStore {

	private final RefreshTokenRepository refreshTokenRepository;

	@Override
	public Optional<RefreshToken> findByTokenHash(String tokenHash) {
		return refreshTokenRepository.findByTokenHash(tokenHash);
	}

	@Override
	public RefreshToken save(RefreshToken refreshToken) {
		return refreshTokenRepository.save(refreshToken);
	}

	@Override
	public void revokeByMemberId(Long memberId, Instant revokedAt) {
		refreshTokenRepository.revokeByMemberId(memberId, revokedAt);
	}

	@Override
	public void revokeByTokenFamilyId(String tokenFamilyId, Instant revokedAt) {
		refreshTokenRepository.revokeByTokenFamilyId(tokenFamilyId, revokedAt);
	}
}
