package com.tasteam.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.MemberRole;
import com.tasteam.domain.member.entity.oauth.MemberOAuthAccount;
import com.tasteam.domain.member.repository.MemberOAuthAccountRepository;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.jwt.repository.RefreshTokenStore;

@UnitTest
@DisplayName("[유닛](Auth) TestAuthTokenService 단위 테스트")
class TestAuthTokenServiceTest {

	private static final String TEST_AUTH_ACCESS_TOKEN_EXPIRATION_PROPERTY = "tasteam.auth.test.access-token-expiration-ms";
	private static final long STG_DEFAULT_TEST_ACCESS_TOKEN_EXPIRATION_MS = 172800000L;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private MemberOAuthAccountRepository memberOAuthAccountRepository;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	@Mock
	private RefreshTokenStore refreshTokenStore;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private Environment environment;

	@InjectMocks
	private TestAuthTokenService testAuthTokenService;

	@Test
	@DisplayName("stg 프로필에서는 테스트 액세스 토큰 만료시간 기본값 48시간을 적용한다")
	void issueTokens_whenStgProfile_uses48HoursAsDefaultExpiration() {
		// given
		MemberOAuthAccount account = mockExistingAccount(7L);
		given(memberOAuthAccountRepository.findByProviderAndProviderUserId("TEST", "test-user-001"))
			.willReturn(Optional.of(account));
		given(environment.acceptsProfiles(any(Profiles.class))).willReturn(true);
		given(environment.getProperty(
			TEST_AUTH_ACCESS_TOKEN_EXPIRATION_PROPERTY,
			Long.class,
			STG_DEFAULT_TEST_ACCESS_TOKEN_EXPIRATION_MS)).willReturn(STG_DEFAULT_TEST_ACCESS_TOKEN_EXPIRATION_MS);
		given(jwtTokenProvider.generateAccessToken(7L, "USER", STG_DEFAULT_TEST_ACCESS_TOKEN_EXPIRATION_MS))
			.willReturn("access-token");
		given(jwtTokenProvider.generateRefreshToken(7L)).willReturn("refresh-token");
		given(jwtTokenProvider.getExpiration("refresh-token")).willReturn(Date.from(Instant.now().plusSeconds(3600)));

		// when
		TestAuthTokenService.TestTokenResult result = testAuthTokenService.issueTokens("test-user-001", "부하테스트계정");

		// then
		assertThat(result.accessToken()).isEqualTo("access-token");
		then(jwtTokenProvider).should().generateAccessToken(7L, "USER", STG_DEFAULT_TEST_ACCESS_TOKEN_EXPIRATION_MS);
		then(jwtTokenProvider).should(never()).generateAccessToken(7L, "USER");
	}

	@Test
	@DisplayName("stg 프로필이 아니면 기본 액세스 토큰 만료시간을 사용한다")
	void issueTokens_whenNotStgProfile_usesDefaultAccessTokenExpiration() {
		// given
		MemberOAuthAccount account = mockExistingAccount(9L);
		given(memberOAuthAccountRepository.findByProviderAndProviderUserId("TEST", "test-user-002"))
			.willReturn(Optional.of(account));
		given(environment.acceptsProfiles(any(Profiles.class))).willReturn(false);
		given(jwtTokenProvider.generateAccessToken(9L, "USER")).willReturn("access-token");
		given(jwtTokenProvider.generateRefreshToken(9L)).willReturn("refresh-token");
		given(jwtTokenProvider.getExpiration("refresh-token")).willReturn(Date.from(Instant.now().plusSeconds(3600)));

		// when
		TestAuthTokenService.TestTokenResult result = testAuthTokenService.issueTokens("test-user-002", "부하테스트계정");

		// then
		assertThat(result.accessToken()).isEqualTo("access-token");
		then(jwtTokenProvider).should().generateAccessToken(9L, "USER");
		then(jwtTokenProvider).should(never()).generateAccessToken(eq(9L), eq("USER"), anyLong());
		then(environment).should(never())
			.getProperty(TEST_AUTH_ACCESS_TOKEN_EXPIRATION_PROPERTY, Long.class,
				STG_DEFAULT_TEST_ACCESS_TOKEN_EXPIRATION_MS);
	}

	private MemberOAuthAccount mockExistingAccount(Long memberId) {
		Member member = mock(Member.class);
		given(member.getId()).willReturn(memberId);
		given(member.getRole()).willReturn(MemberRole.USER);

		MemberOAuthAccount account = mock(MemberOAuthAccount.class);
		given(account.getMember()).willReturn(member);
		return account;
	}
}
