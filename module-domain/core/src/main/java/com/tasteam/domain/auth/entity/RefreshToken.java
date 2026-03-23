package com.tasteam.domain.auth.entity;

import java.time.Instant;

import com.tasteam.domain.common.BaseCreatedAtEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token", indexes = {
	@Index(name = "idx_refresh_token_member_id", columnList = "member_id"),
	@Index(name = "idx_refresh_token_family_id", columnList = "token_family_id"),
	@Index(name = "idx_refresh_token_expires_at", columnList = "expires_at")
})
public class RefreshToken extends BaseCreatedAtEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "token_family_id", nullable = false, length = 64)
	private String tokenFamilyId;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "rotated_at")
	private Instant rotatedAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	public static RefreshToken issue(Long memberId, String tokenHash, String tokenFamilyId, Instant expiresAt) {
		return RefreshToken.builder()
			.memberId(memberId)
			.tokenHash(tokenHash)
			.tokenFamilyId(tokenFamilyId)
			.expiresAt(expiresAt)
			.build();
	}

	public void rotate(Instant now) {
		this.rotatedAt = now;
	}

	public void revoke(Instant now) {
		this.revokedAt = now;
	}

	public boolean isRotated() {
		return rotatedAt != null;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isExpired(Instant now) {
		return expiresAt.isBefore(now);
	}
}
