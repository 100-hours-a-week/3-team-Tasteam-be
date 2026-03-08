package com.tasteam.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.test.util.ReflectionTestUtils;

import com.tasteam.config.annotation.UnitTest;
import com.tasteam.domain.auth.entity.RefreshToken;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.oauth.MemberOAuthAccount;
import com.tasteam.domain.member.event.MemberRegisteredEvent;
import com.tasteam.domain.member.repository.MemberOAuthAccountRepository;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.fixture.MemberFixture;
import com.tasteam.global.security.jwt.provider.JwtTokenProvider;
import com.tasteam.global.security.jwt.repository.RefreshTokenStore;

@UnitTest
@DisplayName("[유닛](Auth) TestAuthTokenService 단위 테스트")
class TestAuthTokenServiceTest {

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
	private TestAuthTokenService service;

	@BeforeEach
	void setUp() {
		given(environment.acceptsProfiles(any(Profiles.class))).willReturn(false);
		given(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).willReturn("access-token");
		given(jwtTokenProvider.generateRefreshToken(anyLong())).willReturn("refresh-token");
		given(jwtTokenProvider.getExpiration("refresh-token"))
			.willReturn(Date.from(Instant.parse("2099-01-01T00:00:00Z")));
		given(refreshTokenStore.save(any(RefreshToken.class))).willAnswer(invocation -> invocation.getArgument(0));
		given(memberRepository.save(any(Member.class))).willAnswer(invocation -> {
			Member member = invocation.getArgument(0);
			if (member.getId() == null) {
				ReflectionTestUtils.setField(member, "id", 101L);
			}
			return member;
		});
	}

	@Test
	@DisplayName("TEST OAuth 계정이 있으면 기존 회원으로 로그인한다")
	void issueTokens_logsInExistingMemberWhenOAuthAccountExists() {
		// given
		Member member = MemberFixture.createWithId(11L, "dummy-11@dummy.tasteam.kr", "기존회원");
		MemberOAuthAccount oAuthAccount = MemberOAuthAccount.create(
			"TEST",
			"test-user-001",
			"test-user-001@test.local",
			member);
		given(memberOAuthAccountRepository.findByProviderAndProviderUserId("TEST", "test-user-001"))
			.willReturn(Optional.of(oAuthAccount));

		// when
		TestAuthTokenService.TestTokenResult result = service.issueTokens("test-user-001", "무시닉네임");

		// then
		assertThat(result.memberId()).isEqualTo(11L);
		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.refreshToken()).isEqualTo("refresh-token");
		assertThat(result.isNew()).isFalse();
		assertThat(member.getLastLoginAt()).isNotNull();
		verify(memberRepository).save(member);
		verify(memberOAuthAccountRepository, never()).save(any(MemberOAuthAccount.class));
		verify(eventPublisher, never()).publishEvent(any(MemberRegisteredEvent.class));
		verify(refreshTokenStore).revokeByMemberId(eq(11L), any(Instant.class));
		verify(refreshTokenStore).save(any(RefreshToken.class));
	}

	@Test
	@DisplayName("TEST OAuth 계정이 없으면 회원 가입 후 로그인한다")
	void issueTokens_signsUpMemberWhenOAuthAccountDoesNotExist() {
		// given
		given(memberOAuthAccountRepository.findByProviderAndProviderUserId("TEST", "test-user-001"))
			.willReturn(Optional.empty());
		given(memberOAuthAccountRepository.save(any(MemberOAuthAccount.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		TestAuthTokenService.TestTokenResult result = service.issueTokens("test-user-001", "부하테스트계정1");

		// then
		ArgumentCaptor<MemberOAuthAccount> accountCaptor = ArgumentCaptor.forClass(MemberOAuthAccount.class);

		assertThat(result.memberId()).isEqualTo(101L);
		assertThat(result.accessToken()).isEqualTo("access-token");
		assertThat(result.refreshToken()).isEqualTo("refresh-token");
		assertThat(result.isNew()).isTrue();
		verify(memberRepository).save(any(Member.class));
		verify(memberOAuthAccountRepository).save(accountCaptor.capture());
		verify(eventPublisher).publishEvent(any(MemberRegisteredEvent.class));
		verify(refreshTokenStore).revokeByMemberId(eq(101L), any(Instant.class));
		verify(refreshTokenStore).save(any(RefreshToken.class));

		MemberOAuthAccount savedAccount = accountCaptor.getValue();
		assertThat(savedAccount.getProvider()).isEqualTo("TEST");
		assertThat(savedAccount.getProviderUserId()).isEqualTo("test-user-001");
		assertThat(savedAccount.getProviderUserEmail()).isEqualTo("test-user-001@test.local");
		assertThat(savedAccount.getMember().getEmail()).isEqualTo("test-user-001@test.local");
		assertThat(savedAccount.getMember().getNickname()).isEqualTo("부하테스트계정1");
	}
}
