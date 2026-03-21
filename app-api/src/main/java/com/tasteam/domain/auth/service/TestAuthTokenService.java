package com.tasteam.domain.auth.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.auth.entity.RefreshToken;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.oauth.MemberOAuthAccount;
import com.tasteam.domain.member.event.MemberRegisteredEvent;
import com.tasteam.domain.member.repository.MemberOAuthAccountRepository;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.security.jwt.common.RefreshTokenHasher;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.jwt.repository.RefreshTokenStore;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TestAuthTokenService {

	private static final String TEST_PROVIDER = "TEST";
	private static final String STG_PROFILE = "stg";
	private static final String TEST_ACCESS_TOKEN_EXPIRATION_PROPERTY = "tasteam.auth.test.access-token-expiration-ms";
	private static final long STG_DEFAULT_TEST_ACCESS_TOKEN_EXPIRATION_MS = 172800000L; // 48h

	private final MemberRepository memberRepository;
	private final MemberOAuthAccountRepository memberOAuthAccountRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenStore refreshTokenStore;
	private final ApplicationEventPublisher eventPublisher;
	private final Environment environment;

	public TestTokenResult issueTokens(String identifier, String nickname) {
		return memberOAuthAccountRepository.findByProviderAndProviderUserId(TEST_PROVIDER, identifier)
			.map(oAuthAccount -> loginExistingMember(oAuthAccount.getMember()))
			.orElseGet(() -> createAndLogin(identifier, nickname));
	}

	private TestTokenResult loginExistingMember(Member member) {
		if (member.getDeletedAt() != null) {
			member.activate();
		}

		member.loginSuccess();
		memberRepository.save(member);

		String accessToken = issueTestAccessToken(member);
		String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

		String tokenFamilyId = UUID.randomUUID().toString();
		refreshTokenStore.revokeByMemberId(member.getId(), Instant.now());
		refreshTokenStore.save(RefreshToken.issue(
			member.getId(),
			RefreshTokenHasher.hash(refreshToken),
			tokenFamilyId,
			jwtTokenProvider.getExpiration(refreshToken).toInstant()));

		return new TestTokenResult(accessToken, refreshToken, member.getId(), false);
	}

	private TestTokenResult createAndLogin(String identifier, String nickname) {
		String resolvedNickname = resolveNickname(nickname);
		String email = identifier + "@test.local";

		Member member = Member.create(email, resolvedNickname);
		member.loginSuccess();
		memberRepository.save(member);

		MemberOAuthAccount oAuthAccount = MemberOAuthAccount.create(TEST_PROVIDER, identifier, email, member);
		memberOAuthAccountRepository.save(oAuthAccount);

		eventPublisher.publishEvent(
			new MemberRegisteredEvent(member.getId(), member.getEmail(),
				member.getNickname(), Instant.now()));

		String accessToken = issueTestAccessToken(member);
		String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

		String tokenFamilyId = UUID.randomUUID().toString();
		refreshTokenStore.revokeByMemberId(member.getId(), Instant.now());
		refreshTokenStore.save(RefreshToken.issue(
			member.getId(),
			RefreshTokenHasher.hash(refreshToken),
			tokenFamilyId,
			jwtTokenProvider.getExpiration(refreshToken).toInstant()));

		return new TestTokenResult(accessToken, refreshToken, member.getId(), true);
	}

	private String resolveNickname(String nickname) {
		if (nickname != null && !nickname.isBlank()) {
			return nickname;
		}
		return "테스트유저_" + UUID.randomUUID().toString().substring(0, 8);
	}

	private String issueTestAccessToken(Member member) {
		if (environment.acceptsProfiles(Profiles.of(STG_PROFILE))) {
			long accessTokenExpirationMs = environment.getProperty(
				TEST_ACCESS_TOKEN_EXPIRATION_PROPERTY,
				Long.class,
				STG_DEFAULT_TEST_ACCESS_TOKEN_EXPIRATION_MS);
			return jwtTokenProvider.generateAccessToken(member.getId(), member.getRole().name(),
				accessTokenExpirationMs);
		}
		return jwtTokenProvider.generateAccessToken(member.getId(), member.getRole().name());
	}

	public record TestTokenResult(String accessToken, String refreshToken, Long memberId, boolean isNew) {
	}
}
