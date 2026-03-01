package com.tasteam.domain.auth.service;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.oauth.MemberOAuthAccount;
import com.tasteam.domain.member.event.MemberRegisteredEvent;
import com.tasteam.domain.member.repository.MemberOAuthAccountRepository;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LocalAuthTokenService {

	private static final String DEV_PROVIDER = "DEV";

	private final MemberRepository memberRepository;
	private final MemberOAuthAccountRepository memberOAuthAccountRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final ApplicationEventPublisher eventPublisher;

	public TokenPair issueTokens(String email, String nickname) {
		boolean[] isNew = {false};
		Member member = memberRepository.findByEmail(email)
			.orElseGet(() -> {
				isNew[0] = true;
				return registerMember(email, nickname);
			});

		ensureDevOAuthAccount(member, email);
		member.loginSuccess();
		memberRepository.save(member);

		if (isNew[0]) {
			eventPublisher.publishEvent(
				new MemberRegisteredEvent(member.getId(), member.getEmail(),
					member.getNickname(), Instant.now()));
		}

		String accessToken = jwtTokenProvider.generateAccessToken(member.getId(), member.getRole().name());
		String refreshToken = jwtTokenProvider.generateRefreshToken(member.getId());

		return new TokenPair(accessToken, refreshToken, member.getId());
	}

	private Member registerMember(String email, String nickname) {
		Member member = Member.create(email, nickname);
		member.loginSuccess();
		return memberRepository.save(member);
	}

	private void ensureDevOAuthAccount(Member member, String email) {
		memberOAuthAccountRepository.findByProviderAndProviderUserId(DEV_PROVIDER, email)
			.orElseGet(() -> memberOAuthAccountRepository.save(
				MemberOAuthAccount.create(DEV_PROVIDER, email, email, member)));
	}

	public record TokenPair(String accessToken, String refreshToken, Long memberId) {
	}
}
