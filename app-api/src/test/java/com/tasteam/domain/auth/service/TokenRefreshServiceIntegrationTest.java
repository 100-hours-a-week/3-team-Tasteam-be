package com.tasteam.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.auth.entity.RefreshToken;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.global.exception.business.BusinessException;
import com.tasteam.global.exception.code.AuthErrorCode;
import com.tasteam.global.exception.code.MemberErrorCode;
import com.tasteam.global.security.jwt.common.JwtTokenConstants;
import com.tasteam.global.security.jwt.common.RefreshTokenHasher;
import com.tasteam.global.security.jwt.properties.JwtProperties;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.jwt.repository.RefreshTokenStore;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ServiceIntegrationTest
@Transactional
class TokenRefreshServiceIntegrationTest {

	@Autowired
	private TokenRefreshService tokenRefreshService;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private JwtProperties jwtProperties;

	@Autowired
	private RefreshTokenStore refreshTokenStore;

	@Autowired
	private MemberRepository memberRepository;

	@Nested
	@DisplayName("리프레시 토큰 재발급")
	class RefreshTokens {

		@Test
		@DisplayName("유효한 리프레시 토큰이면 새로운 액세스 토큰이 발급된다")
		void refreshTokensSuccess() {
			Member member = memberRepository.save(MemberFixture.create("refresh@example.com", "refresh"));
			String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());
			saveRefreshToken(member.getId(), refreshToken);

			TokenRefreshService.TokenPair tokenPair = tokenRefreshService.refreshTokens(refreshToken);

			assertThat(tokenPair.accessToken()).isNotBlank();
			assertThat(tokenPair.refreshToken()).isEqualTo(refreshToken);
		}

		@Test
		@DisplayName("리프레시 토큰 형식이 아니면 실패한다")
		void refreshTokensInvalidTypeFails() {
			Member member = memberRepository.save(MemberFixture.create("access@example.com", "access"));
			String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getRole().name());

			assertThatThrownBy(() -> tokenRefreshService.refreshTokens(accessToken))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID.name());
		}

		@Test
		@DisplayName("만료된 리프레시 토큰이면 실패한다")
		void refreshTokensExpiredFails() {
			Member member = memberRepository.save(MemberFixture.create("expired@example.com", "expired"));
			String expiredRefreshToken = createExpiredRefreshToken(member.getId());

			assertThatThrownBy(() -> tokenRefreshService.refreshTokens(expiredRefreshToken))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED.name());
		}

		@Test
		@DisplayName("비활성 회원이면 실패한다")
		void refreshTokensInactiveMemberFails() {
			Member member = memberRepository.save(MemberFixture.createBlocked());
			String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());
			saveRefreshToken(member.getId(), refreshToken);

			assertThatThrownBy(() -> tokenRefreshService.refreshTokens(refreshToken))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(MemberErrorCode.MEMBER_INACTIVE.name());
		}

		@Test
		@DisplayName("저장된 리프레시 토큰이 없으면 실패한다")
		void refreshTokensMissingStoredTokenFails() {
			Member member = memberRepository.save(MemberFixture.create("missing@example.com", "missing"));
			String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

			assertThatThrownBy(() -> tokenRefreshService.refreshTokens(refreshToken))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSED.name());
		}
	}

	private void saveRefreshToken(Long memberId, String refreshToken) {
		String tokenHash = RefreshTokenHasher.hash(refreshToken);
		RefreshToken stored = RefreshToken.issue(
			memberId,
			tokenHash,
			"token-family-" + memberId,
			jwtTokenProvider.getExpiration(refreshToken).toInstant());
		refreshTokenStore.save(stored);
	}

	private String createExpiredRefreshToken(Long memberId) {
		SecretKey secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
		Instant issuedAt = Instant.now().minusSeconds(120);
		Instant expiredAt = Instant.now().minusSeconds(60);
		return Jwts.builder()
			.subject(String.valueOf(memberId))
			.id("expired-token-id")
			.claim(JwtTokenConstants.CLAIM_TYPE, JwtTokenConstants.TOKEN_TYPE_REFRESH)
			.issuedAt(Date.from(issuedAt))
			.expiration(Date.from(expiredAt))
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();
	}
}
