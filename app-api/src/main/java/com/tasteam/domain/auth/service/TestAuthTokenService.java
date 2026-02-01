package com.tasteam.domain.auth.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.auth.entity.RefreshToken;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.oauth.MemberOAuthAccount;
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

	private final MemberRepository memberRepository;
	private final MemberOAuthAccountRepository memberOAuthAccountRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenStore refreshTokenStore;

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

		String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getRole().name());
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

		String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getRole().name());
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

	public record TestTokenResult(String accessToken, String refreshToken, Long memberId, boolean isNew) {
	}
}
