package com.tasteam.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.tasteam.config.annotation.UnitTest;

@UnitTest
@DisplayName("리프레쉬 토큰 엔티티")
class RefreshTokenTest {

	private static final Long DEFAULT_MEMBER_ID = 1L;
	private static final String DEFAULT_TOKEN_HASH = "abc123hash";
	private static final String DEFAULT_FAMILY_ID = "family-abc-123";
	private static final Instant BASE_TIME = Instant.parse("2026-01-01T12:00:00Z");
	private static final Instant FUTURE_TIME = Instant.parse("2026-01-02T12:00:00Z");
	private static final Instant PAST_TIME = Instant.parse("2025-12-31T12:00:00Z");

	private RefreshToken issueToken(Instant expiresAt) {
		return RefreshToken.issue(DEFAULT_MEMBER_ID, DEFAULT_TOKEN_HASH, DEFAULT_FAMILY_ID, expiresAt);
	}

	@Nested
	@DisplayName("토큰 발급")
	class IssueToken {

		@Test
		@DisplayName("토큰을 발급하면 rotatedAt과 revokedAt이 null인 상태로 생성된다")
		void issue_validParams_createsTokenWithNullRotatedAtAndRevokedAt() {
			RefreshToken token = issueToken(FUTURE_TIME);

			assertThat(token.getMemberId()).isEqualTo(DEFAULT_MEMBER_ID);
			assertThat(token.getTokenHash()).isEqualTo(DEFAULT_TOKEN_HASH);
			assertThat(token.getTokenFamilyId()).isEqualTo(DEFAULT_FAMILY_ID);
			assertThat(token.getExpiresAt()).isEqualTo(FUTURE_TIME);
			assertThat(token.getRotatedAt()).isNull();
			assertThat(token.getRevokedAt()).isNull();
		}
	}

	@Nested
	@DisplayName("토큰 상태 전이")
	class TokenStateTransition {

		@Test
		@DisplayName("토큰을 회전하면 rotatedAt이 설정된다")
		void rotate_setsRotatedAt() {
			RefreshToken token = issueToken(FUTURE_TIME);

			token.rotate(BASE_TIME);

			assertThat(token.getRotatedAt()).isEqualTo(BASE_TIME);
		}

		@Test
		@DisplayName("토큰을 폐기하면 revokedAt이 설정된다")
		void revoke_setsRevokedAt() {
			RefreshToken token = issueToken(FUTURE_TIME);

			token.revoke(BASE_TIME);

			assertThat(token.getRevokedAt()).isEqualTo(BASE_TIME);
		}

		@Test
		@DisplayName("회전되지 않은 토큰은 isRotated가 false를 반환한다")
		void isRotated_returnsFalse_whenNotRotated() {
			RefreshToken token = issueToken(FUTURE_TIME);

			assertThat(token.isRotated()).isFalse();
		}

		@Test
		@DisplayName("폐기되지 않은 토큰은 isRevoked가 false를 반환한다")
		void isRevoked_returnsFalse_whenNotRevoked() {
			RefreshToken token = issueToken(FUTURE_TIME);

			assertThat(token.isRevoked()).isFalse();
		}
	}

	@Nested
	@DisplayName("토큰 만료 판단")
	class TokenExpiration {

		@Test
		@DisplayName("만료 시간이 현재 시간보다 과거이면 만료된 토큰이다")
		void isExpired_returnsTrue_whenExpiresAtIsPast() {
			RefreshToken token = issueToken(PAST_TIME);

			assertThat(token.isExpired(BASE_TIME)).isTrue();
		}

		@Test
		@DisplayName("만료 시간이 현재 시간보다 미래이면 만료되지 않은 토큰이다")
		void isExpired_returnsFalse_whenExpiresAtIsFuture() {
			RefreshToken token = issueToken(FUTURE_TIME);

			assertThat(token.isExpired(BASE_TIME)).isFalse();
		}

		@Test
		@DisplayName("만료 시간이 현재 시간과 동일하면 만료되지 않은 토큰이다")
		void isExpired_returnsFalse_whenExpiresAtEqualsNow() {
			RefreshToken token = issueToken(BASE_TIME);

			assertThat(token.isExpired(BASE_TIME)).isFalse();
		}
	}
}
