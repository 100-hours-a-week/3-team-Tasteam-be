package com.tasteam.domain.auth.store;

import java.time.Instant;
import java.util.Optional;

import com.tasteam.domain.auth.entity.RefreshToken;

public interface RefreshTokenStore {
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

	RefreshToken save(RefreshToken refreshToken);

	void revokeByMemberId(Long memberId, Instant revokedAt);

	void revokeByTokenFamilyId(String tokenFamilyId, Instant revokedAt);
}
