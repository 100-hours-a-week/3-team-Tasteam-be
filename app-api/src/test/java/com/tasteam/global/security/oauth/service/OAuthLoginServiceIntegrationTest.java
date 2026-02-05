package com.tasteam.global.security.oauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import com.tasteam.config.annotation.ServiceIntegrationTest;
import com.tasteam.domain.member.entity.Member;
import com.tasteam.domain.member.entity.oauth.MemberOAuthAccount;
import com.tasteam.domain.member.repository.MemberOAuthAccountRepository;
import com.tasteam.domain.member.repository.MemberRepository;
import com.tasteam.fixture.MemberFixture;

@ServiceIntegrationTest
@Transactional
class OAuthLoginServiceIntegrationTest {

	private static final String USER_INFO_URI = "https://oauth.test/userinfo";
	private MockRestServiceServer server;
	private RestTemplate restTemplate;

	@Autowired
	private OAuthLoginService oAuthLoginService;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MemberOAuthAccountRepository memberOAuthAccountRepository;

	@BeforeEach
	void setUpRestClient() {
		// DefaultOAuth2UserService 내부 restOperations와 동일 인스턴스에 mock 서버를 바인딩해야 한다.
		RestOperations currentOps = (RestOperations)ReflectionTestUtils.getField(oAuthLoginService, "restOperations");
		if (currentOps instanceof RestTemplate template) {
			restTemplate = template;
		} else {
			restTemplate = new RestTemplate();
			oAuthLoginService.setRestOperations(restTemplate);
		}
		server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
	}

	@Nested
	@DisplayName("OAuth 로그인")
	class OAuthLogin {

		@Test
		@DisplayName("신규 사용자는 회원 생성 후 로그인 처리된다")
		void oauthLoginCreatesMember() {
			configureMockUserInfoResponse();

			OAuth2UserRequest request = createUserRequest("google");
			OAuth2User user = oAuthLoginService.loadUser(request);

			assertThat(user.getName()).isNotBlank();
			assertThat(memberRepository.findByEmail("oauth@example.com")).isPresent();
			assertThat(memberOAuthAccountRepository.findByProviderAndProviderUserId("google", "google-123"))
				.isPresent();

			server.verify();
		}

		@Test
		@DisplayName("기존 사용자는 로그인 처리된다")
		void oauthLoginExistingMember() {
			Member member = memberRepository.save(MemberFixture.create("oauth@example.com", "oauth-user"));
			memberOAuthAccountRepository.save(
				MemberOAuthAccount.create("google", "google-123", "oauth@example.com", member));

			configureMockUserInfoResponse();

			OAuth2UserRequest request = createUserRequest("google");
			oAuthLoginService.loadUser(request);

			Member updated = memberRepository.findById(member.getId()).orElseThrow();
			assertThat(updated.getLastLoginAt()).isNotNull();

			server.verify();
		}

		@Test
		@DisplayName("탈퇴 상태 회원은 재활성화된다")
		void oauthLoginReactivatesWithdrawnMember() {
			Member member = MemberFixture.createWithdrawn();
			member = memberRepository.save(member);
			memberOAuthAccountRepository.save(
				MemberOAuthAccount.create("google", "google-123", "oauth@example.com", member));

			configureMockUserInfoResponse();

			OAuth2UserRequest request = createUserRequest("google");
			oAuthLoginService.loadUser(request);

			Member updated = memberRepository.findById(member.getId()).orElseThrow();
			assertThat(updated.isActive()).isTrue();
			assertThat(updated.getDeletedAt()).isNull();

			server.verify();
		}

		@Test
		@DisplayName("지원하지 않는 OAuth 공급자이면 실패한다")
		void oauthLoginUnsupportedProviderFails() {
			configureMockUserInfoResponse();

			OAuth2UserRequest request = createUserRequest("unknown");

			assertThatThrownBy(() -> oAuthLoginService.loadUser(request))
				.isInstanceOf(IllegalArgumentException.class);

			server.verify();
		}
	}

	private void configureMockUserInfoResponse() {
		server.expect(requestTo(USER_INFO_URI))
			.andRespond(withSuccess(
				"{\"sub\":\"google-123\",\"email\":\"oauth@example.com\",\"name\":\"OAuth User\"}",
				MediaType.APPLICATION_JSON));
	}

	private OAuth2UserRequest createUserRequest(String registrationId) {
		Builder builder = ClientRegistration.withRegistrationId(registrationId)
			.clientId("client-id")
			.clientSecret("client-secret")
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.redirectUri("https://client.test/callback")
			.authorizationUri("https://oauth.test/auth")
			.tokenUri("https://oauth.test/token")
			.userInfoUri(USER_INFO_URI)
			.userNameAttributeName("sub")
			.scope("profile", "email")
			.clientName("test-client");

		OAuth2AccessToken accessToken = new OAuth2AccessToken(
			TokenType.BEARER,
			"access-token",
			Instant.now(),
			Instant.now().plusSeconds(60));

		return new OAuth2UserRequest(builder.build(), accessToken);
	}
}
