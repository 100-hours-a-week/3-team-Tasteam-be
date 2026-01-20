package com.tasteam.domain.auth.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tasteam.domain.auth.entity.RefreshToken;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select rt from RefreshToken rt where rt.tokenHash = :tokenHash")
	Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash")
	String tokenHash);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update RefreshToken rt
			   set rt.revokedAt = :revokedAt
			 where rt.memberId = :memberId
			   and rt.revokedAt is null
			""")
	int revokeByMemberId(@Param("memberId")
	Long memberId, @Param("revokedAt")
	Instant revokedAt);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update RefreshToken rt
			   set rt.revokedAt = :revokedAt
			 where rt.tokenFamilyId = :tokenFamilyId
			   and rt.revokedAt is null
			""")
	int revokeByTokenFamilyId(@Param("tokenFamilyId")
	String tokenFamilyId,
			@Param("revokedAt")
			Instant revokedAt);
}
